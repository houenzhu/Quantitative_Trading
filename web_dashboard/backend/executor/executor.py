import logging
from datetime import datetime
from typing import Optional
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick, Order, OrderSide, OrderType, OrderStatus
from risk_manager import RiskManager

logger = logging.getLogger(__name__)


class TradeExecutor:
    def __init__(self, risk_manager: RiskManager):
        self.risk_manager = risk_manager
        self.orders = []
        self.order_counter = 0
    
    def generate_order_id(self) -> str:
        self.order_counter += 1
        return f"ORD{datetime.now().strftime('%Y%m%d%H%M%S')}{self.order_counter:04d}"
    
    def execute_order(self, tick: Tick, side: OrderSide, quantity: int, 
                     order_type: OrderType, reason: str) -> Optional[Order]:
        can_trade, msg = self.risk_manager.can_trade(None, side)
        if not can_trade:
            logger.warning(f"风控拒绝: {msg}")
            return None
        
        order = Order(
            order_id=self.generate_order_id(),
            stock_code=tick.stock_code,
            stock_name=tick.stock_name,
            side=side,
            order_type=order_type,
            price=tick.price,
            quantity=quantity,
            status=OrderStatus.EXECUTED,
            created_at=datetime.now(),
            executed_at=datetime.now(),
            executed_price=tick.price,
            reason=reason
        )
        
        self.orders.append(order)
        
        logger.info(f"执行订单: {side.value} {tick.stock_name} {quantity}股 @ {tick.price:.2f} - {reason}")
        
        return order
