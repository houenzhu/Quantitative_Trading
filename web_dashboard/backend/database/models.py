from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional, Dict, Any
from decimal import Decimal


def parse_datetime(value):
    if value is None:
        return None
    if isinstance(value, datetime):
        return value
    if isinstance(value, str):
        try:
            return datetime.fromisoformat(value)
        except ValueError:
            return None
    return None


@dataclass
class AccountState:
    id: Optional[int] = None
    timestamp: datetime = None
    initial_capital: Decimal = Decimal('0')
    current_capital: Decimal = Decimal('0')
    total_equity: Decimal = Decimal('0')
    cash: Decimal = Decimal('0')
    positions_value: Decimal = Decimal('0')
    total_pnl: Decimal = Decimal('0')
    realized_pnl: Decimal = Decimal('0')
    unrealized_pnl: Decimal = Decimal('0')
    daily_pnl: Decimal = Decimal('0')
    daily_loss: Decimal = Decimal('0')
    trade_count: int = 0
    win_count: int = 0
    loss_count: int = 0
    created_at: datetime = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'timestamp': self.timestamp.isoformat() if self.timestamp else None,
            'initial_capital': float(self.initial_capital),
            'current_capital': float(self.current_capital),
            'total_equity': float(self.total_equity),
            'cash': float(self.cash),
            'positions_value': float(self.positions_value),
            'total_pnl': float(self.total_pnl),
            'realized_pnl': float(self.realized_pnl),
            'unrealized_pnl': float(self.unrealized_pnl),
            'daily_pnl': float(self.daily_pnl),
            'daily_loss': float(self.daily_loss),
            'trade_count': self.trade_count,
            'win_count': self.win_count,
            'loss_count': self.loss_count,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'AccountState':
        return cls(
            id=data.get('id'),
            timestamp=parse_datetime(data.get('timestamp')),
            initial_capital=Decimal(str(data.get('initial_capital', 0))),
            current_capital=Decimal(str(data.get('current_capital', 0))),
            total_equity=Decimal(str(data.get('total_equity', 0))),
            cash=Decimal(str(data.get('cash', 0))),
            positions_value=Decimal(str(data.get('positions_value', 0))),
            total_pnl=Decimal(str(data.get('total_pnl', 0))),
            realized_pnl=Decimal(str(data.get('realized_pnl', 0))),
            unrealized_pnl=Decimal(str(data.get('unrealized_pnl', 0))),
            daily_pnl=Decimal(str(data.get('daily_pnl', 0))),
            daily_loss=Decimal(str(data.get('daily_loss', 0))),
            trade_count=data.get('trade_count', 0),
            win_count=data.get('win_count', 0),
            loss_count=data.get('loss_count', 0),
            created_at=parse_datetime(data.get('created_at'))
        )


@dataclass
class Position:
    id: Optional[int] = None
    stock_code: str = ""
    stock_name: str = ""
    quantity: int = 0
    available_quantity: int = 0
    avg_cost: Decimal = Decimal('0')
    current_price: Decimal = Decimal('0')
    market_value: Decimal = Decimal('0')
    unrealized_pnl: Decimal = Decimal('0')
    unrealized_pnl_pct: Decimal = Decimal('0')
    opened_at: datetime = None
    updated_at: datetime = None
    closed_at: datetime = None
    is_active: bool = True
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'stock_code': self.stock_code,
            'stock_name': self.stock_name,
            'quantity': self.quantity,
            'available_quantity': self.available_quantity,
            'avg_cost': float(self.avg_cost),
            'current_price': float(self.current_price),
            'market_value': float(self.market_value),
            'unrealized_pnl': float(self.unrealized_pnl),
            'unrealized_pnl_pct': float(self.unrealized_pnl_pct),
            'opened_at': self.opened_at.isoformat() if self.opened_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None,
            'closed_at': self.closed_at.isoformat() if self.closed_at else None,
            'is_active': self.is_active
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Position':
        return cls(
            id=data.get('id'),
            stock_code=data.get('stock_code', ''),
            stock_name=data.get('stock_name', ''),
            quantity=data.get('quantity', 0),
            available_quantity=data.get('available_quantity', 0),
            avg_cost=Decimal(str(data.get('avg_cost', 0))),
            current_price=Decimal(str(data.get('current_price', 0))),
            market_value=Decimal(str(data.get('market_value', 0))),
            unrealized_pnl=Decimal(str(data.get('unrealized_pnl', 0))),
            unrealized_pnl_pct=Decimal(str(data.get('unrealized_pnl_pct', 0))),
            opened_at=parse_datetime(data.get('opened_at')),
            updated_at=parse_datetime(data.get('updated_at')),
            closed_at=parse_datetime(data.get('closed_at')),
            is_active=bool(data.get('is_active', 1))
        )


@dataclass
class Order:
    id: Optional[int] = None
    order_id: str = ""
    stock_code: str = ""
    stock_name: str = ""
    side: str = ""
    order_type: str = ""
    price: Decimal = Decimal('0')
    quantity: int = 0
    filled_quantity: int = 0
    avg_fill_price: Decimal = Decimal('0')
    status: str = ""
    reason: str = ""
    created_at: datetime = None
    submitted_at: datetime = None
    filled_at: datetime = None
    cancelled_at: datetime = None
    commission: Decimal = Decimal('0')
    slippage: Decimal = Decimal('0')
    error_message: str = ""
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'order_id': self.order_id,
            'stock_code': self.stock_code,
            'stock_name': self.stock_name,
            'side': self.side,
            'order_type': self.order_type,
            'price': float(self.price),
            'quantity': self.quantity,
            'filled_quantity': self.filled_quantity,
            'avg_fill_price': float(self.avg_fill_price),
            'status': self.status,
            'reason': self.reason,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'submitted_at': self.submitted_at.isoformat() if self.submitted_at else None,
            'filled_at': self.filled_at.isoformat() if self.filled_at else None,
            'cancelled_at': self.cancelled_at.isoformat() if self.cancelled_at else None,
            'commission': float(self.commission),
            'slippage': float(self.slippage),
            'error_message': self.error_message
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Order':
        return cls(
            id=data.get('id'),
            order_id=data.get('order_id', ''),
            stock_code=data.get('stock_code', ''),
            stock_name=data.get('stock_name', ''),
            side=data.get('side', ''),
            order_type=data.get('order_type', ''),
            price=Decimal(str(data.get('price', 0))),
            quantity=data.get('quantity', 0),
            filled_quantity=data.get('filled_quantity', 0),
            avg_fill_price=Decimal(str(data.get('avg_fill_price', 0))),
            status=data.get('status', ''),
            reason=data.get('reason', ''),
            created_at=parse_datetime(data.get('created_at')),
            submitted_at=parse_datetime(data.get('submitted_at')),
            filled_at=parse_datetime(data.get('filled_at')),
            cancelled_at=parse_datetime(data.get('cancelled_at')),
            commission=Decimal(str(data.get('commission', 0))),
            slippage=Decimal(str(data.get('slippage', 0))),
            error_message=data.get('error_message', '')
        )


@dataclass
class Trade:
    id: Optional[int] = None
    trade_id: str = ""
    order_id: str = ""
    stock_code: str = ""
    stock_name: str = ""
    side: str = ""
    quantity: int = 0
    price: Decimal = Decimal('0')
    amount: Decimal = Decimal('0')
    commission: Decimal = Decimal('0')
    stamp_duty: Decimal = Decimal('0')
    transfer_fee: Decimal = Decimal('0')
    slippage_cost: Decimal = Decimal('0')
    realized_pnl: Decimal = Decimal('0')
    traded_at: datetime = None
    strategy_reason: str = ""
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'trade_id': self.trade_id,
            'order_id': self.order_id,
            'stock_code': self.stock_code,
            'stock_name': self.stock_name,
            'side': self.side,
            'quantity': self.quantity,
            'price': float(self.price),
            'amount': float(self.amount),
            'commission': float(self.commission),
            'stamp_duty': float(self.stamp_duty),
            'transfer_fee': float(self.transfer_fee),
            'slippage_cost': float(self.slippage_cost),
            'realized_pnl': float(self.realized_pnl),
            'traded_at': self.traded_at.isoformat() if self.traded_at else None,
            'strategy_reason': self.strategy_reason
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Trade':
        return cls(
            id=data.get('id'),
            trade_id=data.get('trade_id', ''),
            order_id=data.get('order_id', ''),
            stock_code=data.get('stock_code', ''),
            stock_name=data.get('stock_name', ''),
            side=data.get('side', ''),
            quantity=data.get('quantity', 0),
            price=Decimal(str(data.get('price', 0))),
            amount=Decimal(str(data.get('amount', 0))),
            commission=Decimal(str(data.get('commission', 0))),
            stamp_duty=Decimal(str(data.get('stamp_duty', 0))),
            transfer_fee=Decimal(str(data.get('transfer_fee', 0))),
            slippage_cost=Decimal(str(data.get('slippage_cost', 0))),
            realized_pnl=Decimal(str(data.get('realized_pnl', 0))),
            traded_at=parse_datetime(data.get('traded_at')),
            strategy_reason=data.get('strategy_reason', '')
        )


@dataclass
class EquitySnapshot:
    id: Optional[int] = None
    snapshot_date: str = ""
    timestamp: datetime = None
    total_equity: Decimal = Decimal('0')
    cash: Decimal = Decimal('0')
    positions_value: Decimal = Decimal('0')
    daily_pnl: Decimal = Decimal('0')
    daily_pnl_pct: Decimal = Decimal('0')
    cumulative_pnl: Decimal = Decimal('0')
    cumulative_pnl_pct: Decimal = Decimal('0')
    drawdown: Decimal = Decimal('0')
    drawdown_pct: Decimal = Decimal('0')
    peak_equity: Decimal = Decimal('0')
    trade_count_today: int = 0
    position_count: int = 0
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'snapshot_date': self.snapshot_date,
            'timestamp': self.timestamp.isoformat() if self.timestamp else None,
            'total_equity': float(self.total_equity),
            'cash': float(self.cash),
            'positions_value': float(self.positions_value),
            'daily_pnl': float(self.daily_pnl),
            'daily_pnl_pct': float(self.daily_pnl_pct),
            'cumulative_pnl': float(self.cumulative_pnl),
            'cumulative_pnl_pct': float(self.cumulative_pnl_pct),
            'drawdown': float(self.drawdown),
            'drawdown_pct': float(self.drawdown_pct),
            'peak_equity': float(self.peak_equity),
            'trade_count_today': self.trade_count_today,
            'position_count': self.position_count
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'EquitySnapshot':
        return cls(
            id=data.get('id'),
            snapshot_date=data.get('snapshot_date', ''),
            timestamp=parse_datetime(data.get('timestamp')),
            total_equity=Decimal(str(data.get('total_equity', 0))),
            cash=Decimal(str(data.get('cash', 0))),
            positions_value=Decimal(str(data.get('positions_value', 0))),
            daily_pnl=Decimal(str(data.get('daily_pnl', 0))),
            daily_pnl_pct=Decimal(str(data.get('daily_pnl_pct', 0))),
            cumulative_pnl=Decimal(str(data.get('cumulative_pnl', 0))),
            cumulative_pnl_pct=Decimal(str(data.get('cumulative_pnl_pct', 0))),
            drawdown=Decimal(str(data.get('drawdown', 0))),
            drawdown_pct=Decimal(str(data.get('drawdown_pct', 0))),
            peak_equity=Decimal(str(data.get('peak_equity', 0))),
            trade_count_today=data.get('trade_count_today', 0),
            position_count=data.get('position_count', 0)
        )


@dataclass
class StockPoolItem:
    id: Optional[int] = None
    stock_code: str = ""
    stock_name: str = ""
    added_at: datetime = None
    removed_at: datetime = None
    is_active: bool = True
    priority: int = 0
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'stock_code': self.stock_code,
            'stock_name': self.stock_name,
            'added_at': self.added_at.isoformat() if self.added_at else None,
            'removed_at': self.removed_at.isoformat() if self.removed_at else None,
            'is_active': self.is_active,
            'priority': self.priority
        }


@dataclass
class StrategyConfig:
    id: Optional[int] = None
    strategy_name: str = ""
    strategy_type: str = ""
    config_json: str = "{}"
    weights_json: str = "{}"
    is_enabled: bool = True
    created_at: datetime = None
    updated_at: datetime = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'strategy_name': self.strategy_name,
            'strategy_type': self.strategy_type,
            'config_json': self.config_json,
            'weights_json': self.weights_json,
            'is_enabled': self.is_enabled,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None
        }


@dataclass
class SystemConfig:
    id: Optional[int] = None
    config_key: str = ""
    config_value: str = ""
    config_type: str = "string"
    description: str = ""
    updated_at: datetime = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'config_key': self.config_key,
            'config_value': self.config_value,
            'config_type': self.config_type,
            'description': self.description,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None
        }


@dataclass
class RiskLog:
    id: Optional[int] = None
    timestamp: datetime = None
    event_type: str = ""
    severity: str = ""
    stock_code: str = ""
    message: str = ""
    details: str = "{}"
    action_taken: str = ""
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'timestamp': self.timestamp.isoformat() if self.timestamp else None,
            'event_type': self.event_type,
            'severity': self.severity,
            'stock_code': self.stock_code,
            'message': self.message,
            'details': self.details,
            'action_taken': self.action_taken
        }


@dataclass
class SignalLog:
    id: Optional[int] = None
    timestamp: datetime = None
    stock_code: str = ""
    stock_name: str = ""
    strategy_name: str = ""
    action: str = ""
    strength: Decimal = Decimal('0')
    reason: str = ""
    metrics: str = "{}"
    executed: bool = False
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'id': self.id,
            'timestamp': self.timestamp.isoformat() if self.timestamp else None,
            'stock_code': self.stock_code,
            'stock_name': self.stock_name,
            'strategy_name': self.strategy_name,
            'action': self.action,
            'strength': float(self.strength),
            'reason': self.reason,
            'metrics': self.metrics,
            'executed': self.executed
        }
