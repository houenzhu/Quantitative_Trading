from typing import Dict, List
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick
from .base import TradingStrategy


class CompositeStrategy(TradingStrategy):
    def __init__(self, strategies: List[TradingStrategy], weights: List[float] = None):
        super().__init__("复合策略")
        self.strategies = strategies
        self.weights = weights or [1.0 / len(strategies)] * len(strategies)
    
    def analyze(self, tick: Tick, history: List[Tick]) -> Dict:
        signals = []
        total_strength = 0
        
        for strategy, weight in zip(self.strategies, self.weights):
            signal = strategy.analyze(tick, history)
            if signal['action'] != 'HOLD':
                weighted_strength = signal['strength'] * weight
                if signal['action'] == 'BUY':
                    total_strength += weighted_strength
                elif signal['action'] == 'SELL':
                    total_strength -= weighted_strength
                signals.append(signal)
        
        if total_strength > 0.3:
            return {
                'action': 'BUY',
                'strength': min(1.0, total_strength),
                'reason': f'综合信号 {len(signals)}个策略共振',
                'signals': signals
            }
        elif total_strength < -0.3:
            return {
                'action': 'SELL',
                'strength': min(1.0, abs(total_strength)),
                'reason': f'综合信号 {len(signals)}个策略共振',
                'signals': signals
            }
        
        return {'action': 'HOLD', 'strength': 0, 'reason': '无明确信号'}
