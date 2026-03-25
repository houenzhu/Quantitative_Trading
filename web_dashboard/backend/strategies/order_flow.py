from typing import Dict, List
from collections import deque
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick
from .base import TradingStrategy


class OrderFlowStrategy(TradingStrategy):
    def __init__(
        self, 
        imbalance_threshold: float = 0.6,
        accumulation_window: int = 20,
        large_order_threshold: float = 3.0,
        pressure_threshold: float = 0.7
    ):
        super().__init__("订单流策略")
        self.imbalance_threshold = imbalance_threshold
        self.accumulation_window = accumulation_window
        self.large_order_threshold = large_order_threshold
        self.pressure_threshold = pressure_threshold
        
        self.imbalance_history: deque = deque(maxlen=accumulation_window)
        self.buy_volume_history: deque = deque(maxlen=accumulation_window)
        self.sell_volume_history: deque = deque(maxlen=accumulation_window)
        self.total_volume_history: deque = deque(maxlen=accumulation_window)
        self.large_order_history: deque = deque(maxlen=50)
    
    def calculate_imbalance(self, tick: Tick) -> float:
        total_buy = tick.buy_volume
        total_sell = tick.sell_volume
        
        if total_buy + total_sell == 0:
            return 0
        
        return (total_buy - total_sell) / (total_buy + total_sell)
    
    def calculate_cumulative_imbalance(self) -> float:
        if not self.imbalance_history:
            return 0
        
        weights = [i + 1 for i in range(len(self.imbalance_history))]
        weighted_sum = sum(imb * w for imb, w in zip(self.imbalance_history, weights))
        weight_sum = sum(weights)
        
        return weighted_sum / weight_sum if weight_sum > 0 else 0
    
    def detect_large_order(self, tick: Tick) -> Dict:
        if not self.total_volume_history:
            return {'is_large': False, 'ratio': 1.0}
        
        avg_volume = sum(self.total_volume_history) / len(self.total_volume_history)
        current_volume = tick.buy_volume + tick.sell_volume
        
        if avg_volume == 0:
            return {'is_large': False, 'ratio': 1.0}
        
        ratio = current_volume / avg_volume
        is_large = ratio > self.large_order_threshold
        
        if is_large:
            self.large_order_history.append({
                'timestamp': tick.timestamp,
                'volume': current_volume,
                'ratio': ratio,
                'buy_ratio': tick.buy_volume / current_volume if current_volume > 0 else 0.5
            })
        
        return {'is_large': is_large, 'ratio': ratio}
    
    def calculate_buying_pressure(self) -> float:
        if not self.buy_volume_history or not self.sell_volume_history:
            return 0.5
        
        total_buy = sum(self.buy_volume_history)
        total_sell = sum(self.sell_volume_history)
        total = total_buy + total_sell
        
        if total == 0:
            return 0.5
        
        return total_buy / total
    
    def detect_order_clustering(self) -> str:
        if len(self.imbalance_history) < 5:
            return 'neutral'
        
        recent_imbalances = list(self.imbalance_history)[-5:]
        avg_imbalance = sum(recent_imbalances) / len(recent_imbalances)
        
        positive_count = sum(1 for imb in recent_imbalances if imb > 0.2)
        negative_count = sum(1 for imb in recent_imbalances if imb < -0.2)
        
        if positive_count >= 4 and avg_imbalance > 0.3:
            return 'strong_buy'
        elif positive_count >= 3 and avg_imbalance > 0.1:
            return 'buy'
        elif negative_count >= 4 and avg_imbalance < -0.3:
            return 'strong_sell'
        elif negative_count >= 3 and avg_imbalance < -0.1:
            return 'sell'
        
        return 'neutral'
    
    def analyze_large_orders(self) -> Dict:
        if not self.large_order_history:
            return {'buy_large': 0, 'sell_large': 0, 'avg_ratio': 0}
        
        recent_large = list(self.large_order_history)[-10:]
        
        buy_large = sum(1 for order in recent_large if order['buy_ratio'] > 0.6)
        sell_large = sum(1 for order in recent_large if order['buy_ratio'] < 0.4)
        avg_ratio = sum(order['ratio'] for order in recent_large) / len(recent_large)
        
        return {
            'buy_large': buy_large,
            'sell_large': sell_large,
            'avg_ratio': avg_ratio
        }
    
    def calculate_order_flow_momentum(self) -> float:
        if len(self.imbalance_history) < 10:
            return 0
        
        recent = list(self.imbalance_history)[-10:]
        older = list(self.imbalance_history)[-20:-10] if len(self.imbalance_history) >= 20 else recent
        
        recent_avg = sum(recent) / len(recent)
        older_avg = sum(older) / len(older) if older else 0
        
        return recent_avg - older_avg
    
    def analyze(self, tick: Tick, history: List[Tick]) -> Dict:
        current_imbalance = self.calculate_imbalance(tick)
        
        self.imbalance_history.append(current_imbalance)
        self.buy_volume_history.append(tick.buy_volume)
        self.sell_volume_history.append(tick.sell_volume)
        self.total_volume_history.append(tick.buy_volume + tick.sell_volume)
        
        cumulative_imbalance = self.calculate_cumulative_imbalance()
        large_order_info = self.detect_large_order(tick)
        buying_pressure = self.calculate_buying_pressure()
        clustering = self.detect_order_clustering()
        large_order_analysis = self.analyze_large_orders()
        flow_momentum = self.calculate_order_flow_momentum()
        
        signal_strength = 0
        action = 'HOLD'
        reasons = []
        
        buy_score = 0
        sell_score = 0
        
        if current_imbalance > self.imbalance_threshold:
            buy_score += 2
            reasons.append(f'买方强势({current_imbalance:.2f})')
        elif current_imbalance < -self.imbalance_threshold:
            sell_score += 2
            reasons.append(f'卖方强势({abs(current_imbalance):.2f})')
        
        if cumulative_imbalance > self.imbalance_threshold:
            buy_score += 2
            reasons.append(f'累积买盘({cumulative_imbalance:.2f})')
        elif cumulative_imbalance < -self.imbalance_threshold:
            sell_score += 2
            reasons.append(f'累积卖盘({abs(cumulative_imbalance):.2f})')
        
        if buying_pressure > self.pressure_threshold:
            buy_score += 1
            reasons.append(f'买入压力({buying_pressure*100:.1f}%)')
        elif buying_pressure < (1 - self.pressure_threshold):
            sell_score += 1
            reasons.append(f'卖出压力({(1-buying_pressure)*100:.1f}%)')
        
        if clustering == 'strong_buy':
            buy_score += 3
            reasons.append('订单聚集-强买')
        elif clustering == 'buy':
            buy_score += 1
            reasons.append('订单聚集-买')
        elif clustering == 'strong_sell':
            sell_score += 3
            reasons.append('订单聚集-强卖')
        elif clustering == 'sell':
            sell_score += 1
            reasons.append('订单聚集-卖')
        
        if large_order_info['is_large']:
            if tick.buy_volume > tick.sell_volume:
                buy_score += 2
                reasons.append(f"大单买入({large_order_info['ratio']:.1f}x)")
            else:
                sell_score += 2
                reasons.append(f"大单卖出({large_order_info['ratio']:.1f}x)")
        
        if large_order_analysis['buy_large'] > large_order_analysis['sell_large'] + 2:
            buy_score += 1
            reasons.append('大单持续买入')
        elif large_order_analysis['sell_large'] > large_order_analysis['buy_large'] + 2:
            sell_score += 1
            reasons.append('大单持续卖出')
        
        if flow_momentum > 0.2:
            buy_score += 1
            reasons.append('订单流动量向上')
        elif flow_momentum < -0.2:
            sell_score += 1
            reasons.append('订单流动量向下')
        
        total_score = buy_score + sell_score
        if total_score > 0:
            if buy_score > sell_score and buy_score >= 3:
                action = 'BUY'
                signal_strength = min(0.9, buy_score / 8)
            elif sell_score > buy_score and sell_score >= 3:
                action = 'SELL'
                signal_strength = min(0.9, sell_score / 8)
        
        return {
            'action': action,
            'strength': signal_strength,
            'reason': ' | '.join(reasons) if reasons else '订单流平衡',
            'metrics': {
                'current_imbalance': round(current_imbalance, 3),
                'cumulative_imbalance': round(cumulative_imbalance, 3),
                'buying_pressure': round(buying_pressure, 3),
                'clustering': clustering,
                'large_order_ratio': round(large_order_info['ratio'], 2),
                'flow_momentum': round(flow_momentum, 3),
                'buy_score': buy_score,
                'sell_score': sell_score
            }
        }
