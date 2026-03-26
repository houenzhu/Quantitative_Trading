import logging
import uuid
from datetime import datetime, date
from decimal import Decimal
from typing import Dict, List, Optional
import json

from database.database import (
    DatabaseManager, AccountStateRepository, PositionRepository,
    OrderRepository, TradeRepository, EquitySnapshotRepository,
    StockPoolRepository, SystemConfigRepository, RiskLogRepository, SignalLogRepository
)
from database.models import (
    AccountState, Position, Order, Trade, 
    EquitySnapshot, StockPoolItem, RiskLog, SignalLog
)

logger = logging.getLogger(__name__)


class PersistenceService:
    def __init__(self, db_config: Dict = None):
        if db_config is None:
            db_config = {
                'host': 'localhost',
                'port': 3306,
                'user': 'root',
                'password': '',
                'database': 'quant_trading'
            }
        
        self.db = DatabaseManager(**db_config)
        
        self.account_repo = AccountStateRepository(self.db)
        self.position_repo = PositionRepository(self.db)
        self.order_repo = OrderRepository(self.db)
        self.trade_repo = TradeRepository(self.db)
        self.equity_repo = EquitySnapshotRepository(self.db)
        self.stock_pool_repo = StockPoolRepository(self.db)
        self.config_repo = SystemConfigRepository(self.db)
        self.risk_log_repo = RiskLogRepository(self.db)
        self.signal_log_repo = SignalLogRepository(self.db)
        
        self._peak_equity = Decimal('0')
        self._daily_trade_count = 0
        self._last_snapshot_date = None
    
    def initialize(self):
        try:
            latest_equity = self.equity_repo.get_latest()
            if latest_equity:
                self._peak_equity = latest_equity.peak_equity
            logger.info("持久化服务初始化完成")
        except Exception as e:
            logger.warning(f"持久化服务初始化警告: {e}")
    
    def save_account_state(self, portfolio_manager) -> int:
        total_equity = portfolio_manager.capital + portfolio_manager.get_total_market_value()
        
        if total_equity > self._peak_equity:
            self._peak_equity = total_equity
        
        state = AccountState(
            timestamp=datetime.now(),
            initial_capital=Decimal(str(portfolio_manager.initial_capital)),
            current_capital=Decimal(str(portfolio_manager.capital)),
            total_equity=Decimal(str(total_equity)),
            cash=Decimal(str(portfolio_manager.capital)),
            positions_value=Decimal(str(portfolio_manager.get_total_market_value())),
            total_pnl=Decimal(str(total_equity - portfolio_manager.initial_capital)),
            realized_pnl=Decimal(str(getattr(portfolio_manager, 'realized_pnl', 0))),
            unrealized_pnl=Decimal(str(portfolio_manager.get_total_profit_loss())),
            daily_pnl=Decimal(str(getattr(portfolio_manager, 'daily_pnl', 0))),
            daily_loss=Decimal(str(getattr(portfolio_manager, 'daily_loss', 0))),
            trade_count=getattr(portfolio_manager, 'trade_count', 0),
            win_count=getattr(portfolio_manager, 'win_count', 0),
            loss_count=getattr(portfolio_manager, 'loss_count', 0)
        )
        
        return self.account_repo.save(state)
    
    def save_position(self, stock_code: str, stock_name: str, quantity: int,
                      avg_cost: Decimal, current_price: Decimal = None) -> int:
        now = datetime.now()
        existing = self.position_repo.get_by_stock_code(stock_code)
        
        if existing:
            position = Position(
                id=existing.id,
                stock_code=stock_code,
                stock_name=stock_name,
                quantity=quantity,
                available_quantity=quantity,
                avg_cost=avg_cost,
                current_price=current_price or existing.current_price,
                market_value=(current_price or existing.current_price) * quantity if quantity > 0 else Decimal('0'),
                opened_at=existing.opened_at,
                updated_at=now,
                is_active=quantity > 0
            )
        else:
            position = Position(
                stock_code=stock_code,
                stock_name=stock_name,
                quantity=quantity,
                available_quantity=quantity,
                avg_cost=avg_cost,
                current_price=current_price or avg_cost,
                market_value=(current_price or avg_cost) * quantity if quantity > 0 else Decimal('0'),
                opened_at=now,
                updated_at=now,
                is_active=quantity > 0
            )
        
        if quantity == 0:
            position.closed_at = now
            position.is_active = False
        
        return self.position_repo.save(position)
    
    def close_position(self, stock_code: str):
        self.position_repo.deactivate(stock_code)
    
    def get_active_positions(self) -> List[Position]:
        return self.position_repo.get_active_positions()
    
    def create_order(self, stock_code: str, stock_name: str, side: str,
                     order_type: str, price: Decimal, quantity: int,
                     reason: str = "") -> Order:
        order = Order(
            order_id=self._generate_order_id(),
            stock_code=stock_code,
            stock_name=stock_name,
            side=side,
            order_type=order_type,
            price=price,
            quantity=quantity,
            filled_quantity=0,
            avg_fill_price=Decimal('0'),
            status='pending',
            reason=reason,
            created_at=datetime.now()
        )
        
        self.order_repo.save(order)
        return order
    
    def update_order_status(self, order_id: str, status: str, 
                            filled_quantity: int = None, 
                            avg_fill_price: Decimal = None,
                            error_message: str = None):
        order = self.order_repo.get_by_order_id(order_id)
        if not order:
            logger.warning(f"订单不存在: {order_id}")
            return
        
        order.status = status
        
        if filled_quantity is not None:
            order.filled_quantity = filled_quantity
        if avg_fill_price is not None:
            order.avg_fill_price = avg_fill_price
        if error_message:
            order.error_message = error_message
        
        if status == 'submitted':
            order.submitted_at = datetime.now()
        elif status == 'filled':
            order.filled_at = datetime.now()
        elif status == 'cancelled':
            order.cancelled_at = datetime.now()
        
        self.order_repo.save(order)
    
    def create_trade(self, order: Order, price: Decimal, quantity: int,
                     realized_pnl: Decimal = Decimal('0')) -> Trade:
        amount = price * quantity
        commission_rate = self.config_repo.get_float('commission_rate', 0.0003)
        min_commission = self.config_repo.get_float('min_commission', 5)
        stamp_duty_rate = self.config_repo.get_float('stamp_duty_rate', 0.001)
        
        commission = max(amount * Decimal(str(commission_rate)), Decimal(str(min_commission)))
        stamp_duty = Decimal('0')
        if order.side == 'sell':
            stamp_duty = amount * Decimal(str(stamp_duty_rate))
        
        trade = Trade(
            trade_id=self._generate_trade_id(),
            order_id=order.order_id,
            stock_code=order.stock_code,
            stock_name=order.stock_name,
            side=order.side,
            quantity=quantity,
            price=price,
            amount=amount,
            commission=commission,
            stamp_duty=stamp_duty,
            transfer_fee=Decimal('0'),
            slippage_cost=Decimal('0'),
            realized_pnl=realized_pnl,
            traded_at=datetime.now(),
            strategy_reason=order.reason
        )
        
        self.trade_repo.save(trade)
        self._daily_trade_count += 1
        
        return trade
    
    def save_equity_snapshot(self, portfolio_manager) -> int:
        today = date.today().isoformat()
        
        if self._last_snapshot_date == today:
            return None
        
        total_equity = portfolio_manager.capital + portfolio_manager.get_total_market_value()
        
        if total_equity > self._peak_equity:
            self._peak_equity = total_equity
        
        drawdown = self._peak_equity - total_equity
        drawdown_pct = (drawdown / self._peak_equity * 100) if self._peak_equity > 0 else Decimal('0')
        
        cumulative_pnl = total_equity - portfolio_manager.initial_capital
        cumulative_pnl_pct = (cumulative_pnl / portfolio_manager.initial_capital * 100) if portfolio_manager.initial_capital > 0 else Decimal('0')
        
        latest_state = self.account_repo.get_latest()
        daily_pnl = Decimal('0')
        daily_pnl_pct = Decimal('0')
        if latest_state:
            daily_pnl = total_equity - latest_state.total_equity
            daily_pnl_pct = (daily_pnl / latest_state.total_equity * 100) if latest_state.total_equity > 0 else Decimal('0')
        
        snapshot = EquitySnapshot(
            snapshot_date=today,
            timestamp=datetime.now(),
            total_equity=total_equity,
            cash=Decimal(str(portfolio_manager.capital)),
            positions_value=Decimal(str(portfolio_manager.get_total_market_value())),
            daily_pnl=daily_pnl,
            daily_pnl_pct=daily_pnl_pct,
            cumulative_pnl=cumulative_pnl,
            cumulative_pnl_pct=cumulative_pnl_pct,
            drawdown=drawdown,
            drawdown_pct=drawdown_pct,
            peak_equity=self._peak_equity,
            trade_count_today=self._daily_trade_count,
            position_count=len(portfolio_manager.positions)
        )
        
        result = self.equity_repo.save(snapshot)
        self._last_snapshot_date = today
        self._daily_trade_count = 0
        
        return result
    
    def save_stock_pool(self, stock_pool: Dict[str, str]):
        self.stock_pool_repo.save_all(stock_pool)
    
    def get_stock_pool(self) -> Dict[str, str]:
        return self.stock_pool_repo.get_active()
    
    def add_stock_to_pool(self, stock_code: str, stock_name: str):
        self.stock_pool_repo.add_stock(stock_code, stock_name)
    
    def remove_stock_from_pool(self, stock_code: str):
        self.stock_pool_repo.remove_stock(stock_code)
    
    def log_risk_event(self, event_type: str, severity: str, message: str,
                       stock_code: str = "", details: Dict = None,
                       action_taken: str = ""):
        log = RiskLog(
            timestamp=datetime.now(),
            event_type=event_type,
            severity=severity,
            stock_code=stock_code,
            message=message,
            details=json.dumps(details, ensure_ascii=False) if details else "{}",
            action_taken=action_taken
        )
        self.risk_log_repo.save(log)
        logger.warning(f"风控事件: [{severity}] {event_type} - {message}")
    
    def log_signal(self, stock_code: str, stock_name: str, strategy_name: str,
                   action: str, strength: Decimal, reason: str,
                   metrics: Dict = None, executed: bool = False):
        log = SignalLog(
            timestamp=datetime.now(),
            stock_code=stock_code,
            stock_name=stock_name,
            strategy_name=strategy_name,
            action=action,
            strength=strength,
            reason=reason,
            metrics=json.dumps(metrics, ensure_ascii=False) if metrics else "{}",
            executed=executed
        )
        self.signal_log_repo.save(log)
    
    def get_config(self, key: str, default: str = None) -> Optional[str]:
        return self.config_repo.get(key, default)
    
    def get_config_float(self, key: str, default: float = 0.0) -> float:
        return self.config_repo.get_float(key, default)
    
    def get_config_int(self, key: str, default: int = 0) -> int:
        return self.config_repo.get_int(key, default)
    
    def set_config(self, key: str, value: str):
        self.config_repo.set(key, value)
    
    def restore_portfolio_state(self, portfolio_manager) -> bool:
        try:
            positions = self.position_repo.get_active_positions()
            for pos in positions:
                portfolio_manager.positions[pos.stock_code] = type('Position', (), {
                    'stock_code': pos.stock_code,
                    'stock_name': pos.stock_name,
                    'quantity': pos.quantity,
                    'avg_price': float(pos.avg_cost),
                    'current_price': float(pos.current_price),
                    'market_value': float(pos.market_value),
                    'profit_loss': float(pos.unrealized_pnl),
                    'profit_loss_pct': float(pos.unrealized_pnl_pct)
                })()
            
            latest_state = self.account_repo.get_latest()
            if latest_state:
                portfolio_manager.capital = float(latest_state.cash)
                portfolio_manager.trade_count = latest_state.trade_count
                portfolio_manager.win_count = latest_state.win_count
                portfolio_manager.loss_count = latest_state.loss_count
                self._peak_equity = latest_state.total_equity
            
            logger.info(f"恢复持仓状态完成: {len(positions)}个持仓")
            return True
        except Exception as e:
            logger.error(f"恢复持仓状态失败: {e}")
            return False
    
    def get_equity_curve(self, days: int = 30) -> List[Dict]:
        snapshots = self.equity_repo.get_by_date_range(days)
        return [s.to_dict() for s in snapshots]
    
    def get_trade_history(self, limit: int = 100) -> List[Dict]:
        trades = self.trade_repo.get_recent_trades(limit)
        return [t.to_dict() for t in trades]
    
    def get_order_history(self, limit: int = 100) -> List[Dict]:
        orders = self.order_repo.get_recent_orders(limit)
        return [o.to_dict() for o in orders]
    
    def close(self):
        self.db.close()
    
    def _generate_order_id(self) -> str:
        return f"ORD{datetime.now().strftime('%Y%m%d%H%M%S')}{uuid.uuid4().hex[:6].upper()}"
    
    def _generate_trade_id(self) -> str:
        return f"TRD{datetime.now().strftime('%Y%m%d%H%M%S')}{uuid.uuid4().hex[:6].upper()}"
