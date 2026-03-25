import logging
from typing import Dict, List
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick, OrderSide, OrderType
from data_fetcher import DataFetcher
from strategies import VWAPStrategy, OrderFlowStrategy, MomentumStrategy, CompositeStrategy
from risk_manager import RiskManager
from executor import TradeExecutor
from portfolio import PortfolioManager
from trading_hours import TradingHours

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
        
        logger.info(f"自动交易系统初始化完成，初始资金: ¥{self.portfolio.initial_capital:,.2f}")
    
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
