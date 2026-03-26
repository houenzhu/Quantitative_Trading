import logging
from typing import Dict, List, Optional
import sys
import os
from datetime import datetime
from decimal import Decimal

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick, OrderSide, OrderType
from data_fetcher import DataFetcher
from strategies import VWAPStrategy, OrderFlowStrategy, MomentumStrategy, CompositeStrategy
from risk_manager import RiskManager
from executor import TradeExecutor
from portfolio import PortfolioManager
from trading_hours import TradingHours
from database import PersistenceService

logger = logging.getLogger(__name__)


class AutoTradingSystem:
    def __init__(self, config: Dict):
        self.config = config
        self.data_fetcher = DataFetcher()
        self.trading_hours = TradingHours()
        
        self.strategies = {
            'vwap': VWAPStrategy(deviation_threshold=config.get('vwap_deviation', 0.005)),
            'orderflow': OrderFlowStrategy(imbalance_threshold=config.get('order_imbalance', 0.6)),
            'momentum': MomentumStrategy(momentum_threshold=config.get('momentum_threshold', 2.0))
        }
        
        self.composite_strategy = CompositeStrategy(
            strategies=list(self.strategies.values()),
            weights=config.get('strategy_weights', [0.4, 0.4, 0.2])
        )
        
        self.risk_manager = RiskManager(config)
        self.executor = TradeExecutor(self.risk_manager)
        self.portfolio = PortfolioManager(config.get('initial_capital', 1000000))
        
        self.history_data: Dict[str, List[Tick]] = {}
        self.running = False
        self.thread = None
        
        self.stock_pool = config.get('stock_pool', {
            '600547': '山东黄金',
            '600489': '中金黄金',
            '601899': '紫金矿业',
            '600988': '赤峰黄金',
            '000975': '银泰黄金',
            '002155': '湖南黄金'
        })
        
        db_config = config.get('database', {
            'host': 'localhost',
            'port': 3306,
            'user': 'root',
            'password': '123456',
            'database': 'trading_system'
        })
        self.persistence = PersistenceService(db_config)
        
        self._init_persistence()
        
        logger.info(f"自动交易系统初始化完成，初始资金: ¥{self.portfolio.initial_capital:,.2f}")
    
    def _init_persistence(self):
        try:
            self.persistence.initialize()
            
            saved_stock_pool = self.persistence.get_stock_pool()
            if saved_stock_pool:
                self.stock_pool = saved_stock_pool
                logger.info(f"从数据库恢复股票池: {len(self.stock_pool)}只股票")
            
            if self.persistence.restore_portfolio_state(self.portfolio):
                logger.info(f"从数据库恢复持仓状态成功")
            
            self.persistence.save_stock_pool(self.stock_pool)
            
        except Exception as e:
            logger.warning(f"持久化初始化失败，将使用默认配置: {e}")
    
    def process_tick(self, tick: Tick):
        if not self.trading_hours.is_trading_time():
            return
        
        if tick.stock_code not in self.history_data:
            self.history_data[tick.stock_code] = []
        
        self.history_data[tick.stock_code].append(tick)
        
        if len(self.history_data[tick.stock_code]) > 500:
            self.history_data[tick.stock_code] = self.history_data[tick.stock_code][-500:]
        
        signal = self.composite_strategy.analyze(tick, self.history_data[tick.stock_code])
        
        if signal['action'] != 'HOLD':
            logger.info(f"{tick.stock_name} - {signal['action']}信号 - 强度:{signal['strength']:.2f} - {signal['reason']}")
            
            self.persistence.log_signal(
                stock_code=tick.stock_code,
                stock_name=tick.stock_name,
                strategy_name='composite',
                action=signal['action'],
                strength=Decimal(str(signal['strength'])),
                reason=signal['reason'],
                metrics=signal.get('metrics'),
                executed=False
            )
            
            position_value = self.portfolio.capital * min(0.1, signal['strength'])
            quantity = int(position_value / tick.price)
            
            if quantity > 0:
                if signal['action'] == 'BUY':
                    order = self.executor.execute_order(
                        tick, OrderSide.BUY, quantity, 
                        OrderType.MARKET, signal['reason']
                    )
                    if order:
                        self.portfolio.update_position(order)
                        self._persist_order(order, tick, signal['reason'])
                elif signal['action'] == 'SELL':
                    if tick.stock_code in self.portfolio.positions:
                        pos = self.portfolio.positions[tick.stock_code]
                        sell_qty = min(quantity, pos.quantity)
                        if sell_qty > 0:
                            order = self.executor.execute_order(
                                tick, OrderSide.SELL, sell_qty,
                                OrderType.MARKET, signal['reason']
                            )
                            if order:
                                self.portfolio.update_position(order)
                                self._persist_order(order, tick, signal['reason'])
        
        self._update_positions_price(tick)
    
    def _persist_order(self, order, tick: Tick, reason: str):
        try:
            db_order = self.persistence.create_order(
                stock_code=order.stock_code,
                stock_name=order.stock_name,
                side='buy' if order.side == OrderSide.BUY else 'sell',
                order_type='market',
                price=Decimal(str(order.executed_price or order.price)),
                quantity=order.quantity,
                reason=reason
            )
            
            self.persistence.update_order_status(
                order_id=db_order.order_id,
                status='filled',
                filled_quantity=order.quantity,
                avg_fill_price=Decimal(str(order.executed_price or order.price))
            )
            
            realized_pnl = Decimal('0')
            if order.side == OrderSide.SELL:
                pos = self.portfolio.positions.get(order.stock_code)
                if pos:
                    realized_pnl = Decimal(str(pos.profit_loss))
            
            trade = self.persistence.create_trade(
                order=db_order,
                price=Decimal(str(order.executed_price or tick.price)),
                quantity=order.quantity,
                realized_pnl=realized_pnl
            )
            
            self.persistence.save_position(
                stock_code=order.stock_code,
                stock_name=order.stock_name,
                quantity=self.portfolio.positions[order.stock_code].quantity if order.stock_code in self.portfolio.positions else 0,
                avg_cost=Decimal(str(self.portfolio.positions[order.stock_code].avg_price)) if order.stock_code in self.portfolio.positions else Decimal(str(order.executed_price)),
                current_price=Decimal(str(tick.price))
            )
            
            self.persistence.save_account_state(self.portfolio)
            
            logger.debug(f"订单持久化完成: {db_order.order_id}")
            
        except Exception as e:
            logger.error(f"订单持久化失败: {e}")
    
    def _update_positions_price(self, tick: Tick):
        if tick.stock_code in self.portfolio.positions:
            pos = self.portfolio.positions[tick.stock_code]
            pos.current_price = tick.price
            pos.market_value = pos.quantity * tick.price
            pos.profit_loss = (tick.price - pos.avg_price) * pos.quantity
            pos.profit_loss_pct = (tick.price - pos.avg_price) / pos.avg_price * 100 if pos.avg_price > 0 else 0
    
    def add_stock(self, stock_code: str, stock_name: str):
        self.stock_pool[stock_code] = stock_name
        self.persistence.add_stock_to_pool(stock_code, stock_name)
        logger.info(f"添加股票到股票池: {stock_code} - {stock_name}")
    
    def remove_stock(self, stock_code: str):
        if stock_code in self.stock_pool:
            del self.stock_pool[stock_code]
            self.persistence.remove_stock_from_pool(stock_code)
            logger.info(f"从股票池移除股票: {stock_code}")
    
    def get_statistics(self) -> Dict:
        total_market_value = self.portfolio.get_total_market_value()
        total_equity = self.portfolio.capital + total_market_value
        total_pnl = total_equity - self.portfolio.initial_capital
        total_pnl_pct = (total_pnl / self.portfolio.initial_capital) * 100 if self.portfolio.initial_capital > 0 else 0
        
        return {
            '总资产': f"¥{total_equity:,.2f}",
            '可用资金': f"¥{self.portfolio.capital:,.2f}",
            '持仓市值': f"¥{total_market_value:,.2f}",
            '总收益': f"¥{total_pnl:,.2f}",
            '总收益率': f"{total_pnl_pct:.2f}%",
            '持仓数量': str(len(self.portfolio.positions)),
            '交易次数': str(getattr(self.portfolio, 'trade_count', 0)),
            '胜率': f"{(getattr(self.portfolio, 'win_count', 0) / max(getattr(self.portfolio, 'trade_count', 1), 1) * 100):.2f}%"
        }
    
    def get_equity_curve(self, days: int = 30) -> List[Dict]:
        return self.persistence.get_equity_curve(days)
    
    def get_trade_history(self, limit: int = 100) -> List[Dict]:
        return self.persistence.get_trade_history(limit)
    
    def get_order_history(self, limit: int = 100) -> List[Dict]:
        return self.persistence.get_order_history(limit)
    
    def save_daily_snapshot(self):
        try:
            self.persistence.save_equity_snapshot(self.portfolio)
            logger.info("每日权益快照保存完成")
        except Exception as e:
            logger.error(f"保存每日快照失败: {e}")
    
    def shutdown(self):
        try:
            self.persistence.save_account_state(self.portfolio)
            
            for code, pos in self.portfolio.positions.items():
                self.persistence.save_position(
                    stock_code=code,
                    stock_name=pos.stock_name,
                    quantity=pos.quantity,
                    avg_cost=Decimal(str(pos.avg_price)),
                    current_price=Decimal(str(pos.current_price))
                )
            
            self.persistence.save_stock_pool(self.stock_pool)
            
            self.persistence.close()
            logger.info("交易系统关闭，数据已持久化")
        except Exception as e:
            logger.error(f"关闭时持久化失败: {e}")
