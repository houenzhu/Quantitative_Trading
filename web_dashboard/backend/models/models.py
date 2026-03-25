from datetime import datetime
from dataclasses import dataclass
from typing import Optional
from enum import Enum


class OrderType(Enum):
    MARKET = "市价单"
    LIMIT = "限价单"
    STOP = "止损单"


class OrderSide(Enum):
    BUY = "买入"
    SELL = "卖出"


class OrderStatus(Enum):
    PENDING = "待执行"
    EXECUTED = "已成交"
    CANCELLED = "已撤销"
    REJECTED = "已拒绝"


@dataclass
class Tick:
    timestamp: datetime
    stock_code: str
    stock_name: str
    price: float
    volume: int
    amount: float
    buy_volume: int
    sell_volume: int
    change_percent: float
    bid_price: float
    ask_price: float


@dataclass
class Order:
    order_id: str
    stock_code: str
    stock_name: str
    side: OrderSide
    order_type: OrderType
    price: float
    quantity: int
    status: OrderStatus
    created_at: datetime
    executed_at: Optional[datetime] = None
    executed_price: Optional[float] = None
    reason: Optional[str] = None


@dataclass
class Position:
    stock_code: str
    stock_name: str
    quantity: int
    avg_price: float
    current_price: float
    market_value: float
    profit_loss: float
    profit_loss_pct: float
