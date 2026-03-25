from typing import Dict, List
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick
from .base import TradingStrategy


class CompositeStrategy(TradingStrategy):
    def __init__(
        self, 
        strategies: List[TradingStrategy], 
        weights: List[float] = None,
        min_agreement: int = 2,
        use_voting: bool = True
    ):
        super().__init__("复合策略")
        self.strategies = strategies
        self.weights = weights or [1.0 / len(strategies)] * len(strategies)
        self.min_agreement = min_agreement
        self.use_voting = use_voting
        
        self.signal_history: List[Dict] = []
    
    def normalize_weights(self):
        total = sum(self.weights)
        if total > 0:
            self.weights = [w / total for w in self.weights]
    
    def weighted_voting(self, signals: List[Dict]) -> Dict:
        buy_score = 0
        sell_score = 0
        buy_weight = 0
        sell_weight = 0
        
        for signal, weight in zip(signals, self.weights):
            if signal['action'] == 'BUY':
                buy_score += 1
                buy_weight += signal['strength'] * weight
            elif signal['action'] == 'SELL':
                sell_score += 1
                sell_weight += signal['strength'] * weight
        
        return {
            'buy_score': buy_score,
            'sell_score': sell_score,
            'buy_weight': buy_weight,
            'sell_weight': sell_weight
        }
    
    def analyze(self, tick: Tick, history: List[Tick]) -> Dict:
        signals = []
        all_metrics = {}
        
        for i, (strategy, weight) in enumerate(zip(self.strategies, self.weights)):
            signal = strategy.analyze(tick, history)
            signal['strategy_name'] = strategy.name
            signal['weight'] = weight
            signals.append(signal)
            
            if 'metrics' in signal:
                all_metrics[f'{strategy.name}_metrics'] = signal['metrics']
        
        if self.use_voting:
            vote_result = self.weighted_voting(signals)
            
            buy_score = vote_result['buy_score']
            sell_score = vote_result['sell_score']
            buy_weight = vote_result['buy_weight']
            sell_weight = vote_result['sell_weight']
            
            action = 'HOLD'
            strength = 0
            reasons = []
            
            if buy_score >= self.min_agreement and buy_weight > sell_weight:
                action = 'BUY'
                strength = min(1.0, buy_weight * 1.2)
                reasons.append(f'{buy_score}个策略买入共振')
                
                if buy_score == len(self.strategies):
                    strength = min(1.0, strength * 1.3)
                    reasons.append('全策略一致')
                    
            elif sell_score >= self.min_agreement and sell_weight > buy_weight:
                action = 'SELL'
                strength = min(1.0, sell_weight * 1.2)
                reasons.append(f'{sell_score}个策略卖出共振')
                
                if sell_score == len(self.strategies):
                    strength = min(1.0, strength * 1.3)
                    reasons.append('全策略一致')
            
            conflicting = buy_score > 0 and sell_score > 0
            if conflicting:
                strength *= 0.7
                reasons.append('策略存在分歧')
        else:
            total_strength = 0
            for signal, weight in zip(signals, self.weights):
                if signal['action'] == 'BUY':
                    total_strength += signal['strength'] * weight
                elif signal['action'] == 'SELL':
                    total_strength -= signal['strength'] * weight
            
            action = 'HOLD'
            strength = 0
            reasons = []
            
            if total_strength > 0.3:
                action = 'BUY'
                strength = min(1.0, total_strength)
                reasons.append(f'综合强度: {total_strength:.2f}')
            elif total_strength < -0.3:
                action = 'SELL'
                strength = min(1.0, abs(total_strength))
                reasons.append(f'综合强度: {abs(total_strength):.2f}')
        
        active_signals = [s for s in signals if s['action'] != 'HOLD']
        
        result = {
            'action': action,
            'strength': strength,
            'reason': ' | '.join(reasons) if reasons else '无明确信号',
            'signals': signals,
            'active_count': len(active_signals)
        }
        
        result.update(all_metrics)
        
        self.signal_history.append({
            'timestamp': tick.timestamp if hasattr(tick, 'timestamp') else None,
            'action': action,
            'strength': strength
        })
        
        if len(self.signal_history) > 100:
            self.signal_history = self.signal_history[-100:]
        
        return result
    
    def get_strategy_performance(self) -> Dict:
        if not self.signal_history:
            return {}
        
        buy_signals = sum(1 for s in self.signal_history if s['action'] == 'BUY')
        sell_signals = sum(1 for s in self.signal_history if s['action'] == 'SELL')
        hold_signals = sum(1 for s in self.signal_history if s['action'] == 'HOLD')
        
        avg_buy_strength = 0
        avg_sell_strength = 0
        
        buy_strengths = [s['strength'] for s in self.signal_history if s['action'] == 'BUY']
        sell_strengths = [s['strength'] for s in self.signal_history if s['action'] == 'SELL']
        
        if buy_strengths:
            avg_buy_strength = sum(buy_strengths) / len(buy_strengths)
        if sell_strengths:
            avg_sell_strength = sum(sell_strengths) / len(sell_strengths)
        
        return {
            'total_signals': len(self.signal_history),
            'buy_signals': buy_signals,
            'sell_signals': sell_signals,
            'hold_signals': hold_signals,
            'avg_buy_strength': round(avg_buy_strength, 3),
            'avg_sell_strength': round(avg_sell_strength, 3)
        }
