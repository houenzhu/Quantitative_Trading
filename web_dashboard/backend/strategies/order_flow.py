from typing import Dict, List
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick
from .base import TradingStrategy


class OrderFlowStrategy(TradingStrategy):
    def __init__(self, imbalance_threshold: float = 0.6):
        super().__init__("订单流策略")
        self.imbalance_threshold = imbalance_threshold
    
    def analyze(self, tick: Tick, history: List[Tick]) -> Dict:
        total_buy = tick.buy_volume
        total_sell = tick.sell_volume
        
        if total_buy + total_sell == 0:
            return {'action': 'HOLD', 'strength': 0, 'reason': '无订单数据'}
        
        imbalance = (total_buy - total_sell) / (total_buy + total_sell)
        
        if imbalance > self.imbalance_threshold:
            return {
                'action': 'BUY',
                'strength': min(0.9, imbalance),
                'reason': f'买方强势 {imbalance:.2f}'
            }
        elif imbalance < -self.imbalance_threshold:
            return {
                'action': 'SELL',
                'strength': min(0.9, abs(imbalance)),
                'reason': f'卖方强势 {abs(imbalance):.2f}'
            }
        
        return {'action': 'HOLD', 'strength': 0, 'reason': '订单流平衡'}
