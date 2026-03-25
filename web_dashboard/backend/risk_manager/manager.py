from typing import Dict, Tuple
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Position, OrderSide


class RiskManager:
    def __init__(self, config: Dict):
        self.config = config
        self.daily_loss = 0
        self.daily_pnl = 0
        self.initial_capital = config.get('initial_capital', 1000000)
        
    def can_trade(self, position: Position, side: OrderSide) -> Tuple[bool, str]:
        if self.daily_loss > self.config.get('max_daily_loss', 0.05) * self.initial_capital:
            return False, "超过单日亏损限制"
        
        max_position_pct = self.config.get('max_position_pct', 0.2)
        if side == OrderSide.BUY:
            position_value = position.market_value if position else 0
            if position_value > self.initial_capital * max_position_pct:
                return False, f"超过单只股票仓位限制 {max_position_pct*100}%"
        
        return True, "通过风控检查"
    
    def update_pnl(self, pnl: float):
        self.daily_pnl += pnl
        if pnl < 0:
            self.daily_loss += abs(pnl)
    
    def reset_daily(self):
        self.daily_loss = 0
        self.daily_pnl = 0
