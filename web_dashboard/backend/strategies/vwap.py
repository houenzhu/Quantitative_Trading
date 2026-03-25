from typing import Dict, List
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick
from .base import TradingStrategy


class VWAPStrategy(TradingStrategy):
    def __init__(self, deviation_threshold: float = 0.005):
        super().__init__("VWAP策略")
        self.deviation_threshold = deviation_threshold
    
    def calculate_vwap(self, history: List[Tick]) -> float:
        if len(history) < 10:
            return 0
        
        total_value = sum(t.price * t.volume for t in history)
        total_volume = sum(t.volume for t in history)
        
        return total_value / total_volume if total_volume > 0 else 0
    
    def analyze(self, tick: Tick, history: List[Tick]) -> Dict:
        vwap = self.calculate_vwap(history)
        
        if vwap == 0:
            return {'action': 'HOLD', 'strength': 0, 'reason': '数据不足'}
        
        deviation = (tick.price - vwap) / vwap
        
        if deviation > self.deviation_threshold:
            return {
                'action': 'SELL',
                'strength': min(0.8, deviation * 100),
                'reason': f'价格高于VWAP {deviation*100:.2f}%'
            }
        elif deviation < -self.deviation_threshold:
            return {
                'action': 'BUY',
                'strength': min(0.8, abs(deviation) * 100),
                'reason': f'价格低于VWAP {abs(deviation)*100:.2f}%'
            }
        
        return {'action': 'HOLD', 'strength': 0, 'reason': '无信号'}
