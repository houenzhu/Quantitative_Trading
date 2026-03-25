from typing import Dict, List
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick
from .base import TradingStrategy


class MomentumStrategy(TradingStrategy):
    def __init__(self, momentum_threshold: float = 2.0):
        super().__init__("动量策略")
        self.momentum_threshold = momentum_threshold
    
    def analyze(self, tick: Tick, history: List[Tick]) -> Dict:
        if len(history) < 20:
            return {'action': 'HOLD', 'strength': 0, 'reason': '历史数据不足'}
        
        short_momentum = tick.change_percent
        long_momentum = (tick.price - history[-20].price) / history[-20].price * 100
        
        if short_momentum > self.momentum_threshold and long_momentum > 0:
            return {
                'action': 'BUY',
                'strength': min(0.7, short_momentum / 10),
                'reason': f'动量强劲 {short_momentum:.2f}%'
            }
        elif short_momentum < -self.momentum_threshold and long_momentum < 0:
            return {
                'action': 'SELL',
                'strength': min(0.7, abs(short_momentum) / 10),
                'reason': f'动量衰竭 {short_momentum:.2f}%'
            }
        
        return {'action': 'HOLD', 'strength': 0, 'reason': '动量正常'}
