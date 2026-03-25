from typing import Dict, List
from collections import deque
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick
from .base import TradingStrategy


class VWAPStrategy(TradingStrategy):
    def __init__(
        self, 
        deviation_threshold: float = 0.005,
        window_size: int = 100,
        volume_spike_threshold: float = 2.0,
        use_rolling: bool = True
    ):
        super().__init__("VWAP策略")
        self.deviation_threshold = deviation_threshold
        self.window_size = window_size
        self.volume_spike_threshold = volume_spike_threshold
        self.use_rolling = use_rolling
        self.price_history: deque = deque(maxlen=window_size)
        self.volume_history: deque = deque(maxlen=window_size)
        self.vwap_cache: Dict[str, float] = {}
    
    def calculate_vwap(self, history: List[Tick]) -> float:
        if len(history) < 10:
            return 0
        
        if self.use_rolling:
            data = list(self.price_history) if self.price_history else history[-self.window_size:]
        else:
            data = history
        
        if not data:
            return 0
        
        total_value = 0
        total_volume = 0
        
        for tick in data[-self.window_size:]:
            total_value += tick.price * tick.volume
            total_volume += tick.volume
        
        return total_value / total_volume if total_volume > 0 else 0
    
    def calculate_session_vwap(self, history: List[Tick]) -> float:
        if len(history) < 5:
            return 0
        
        today_ticks = []
        if history:
            last_date = history[-1].timestamp.date()
            for tick in reversed(history):
                if tick.timestamp.date() == last_date:
                    today_ticks.append(tick)
                else:
                    break
            today_ticks.reverse()
        
        if not today_ticks:
            return 0
        
        total_value = sum(t.price * t.volume for t in today_ticks)
        total_volume = sum(t.volume for t in today_ticks)
        
        return total_value / total_volume if total_volume > 0 else 0
    
    def detect_volume_spike(self, tick: Tick, history: List[Tick]) -> bool:
        if len(history) < 20:
            return False
        
        recent_volumes = [t.volume for t in history[-20:]]
        avg_volume = sum(recent_volumes) / len(recent_volumes)
        
        return tick.volume > avg_volume * self.volume_spike_threshold
    
    def calculate_volume_weighted_deviation(self, tick: Tick, vwap: float, history: List[Tick]) -> float:
        if vwap == 0 or len(history) < 10:
            return 0
        
        price_deviation = (tick.price - vwap) / vwap
        
        volume_weights = []
        for i, t in enumerate(history[-10:]):
            weight = t.volume / (sum(ht.volume for ht in history[-10:]) + 1)
            volume_weights.append(weight)
        
        weighted_deviation = price_deviation * (1 + volume_weights[-1] if volume_weights else 1)
        
        return weighted_deviation
    
    def analyze(self, tick: Tick, history: List[Tick]) -> Dict:
        self.price_history.append(tick)
        self.volume_history.append(tick.volume)
        
        vwap = self.calculate_vwap(history)
        session_vwap = self.calculate_session_vwap(history)
        
        if vwap == 0:
            return {'action': 'HOLD', 'strength': 0, 'reason': '数据不足'}
        
        deviation = (tick.price - vwap) / vwap
        weighted_deviation = self.calculate_volume_weighted_deviation(tick, vwap, history)
        
        is_volume_spike = self.detect_volume_spike(tick, history)
        
        vwap_trend = 0
        if len(history) >= 30:
            old_vwap = self.calculate_vwap(history[:-20])
            if old_vwap > 0:
                vwap_trend = (vwap - old_vwap) / old_vwap
        
        signal_strength = 0
        action = 'HOLD'
        reasons = []
        
        if weighted_deviation > self.deviation_threshold:
            action = 'SELL'
            signal_strength = min(0.9, abs(weighted_deviation) * 50)
            reasons.append(f'价格高于VWAP {deviation*100:.2f}%')
            
            if is_volume_spike:
                signal_strength *= 1.3
                signal_strength = min(1.0, signal_strength)
                reasons.append('成交量异常放大')
            
            if vwap_trend < -0.001:
                signal_strength *= 1.2
                reasons.append('VWAP下行趋势')
                
        elif weighted_deviation < -self.deviation_threshold:
            action = 'BUY'
            signal_strength = min(0.9, abs(weighted_deviation) * 50)
            reasons.append(f'价格低于VWAP {abs(deviation)*100:.2f}%')
            
            if is_volume_spike:
                signal_strength *= 1.3
                signal_strength = min(1.0, signal_strength)
                reasons.append('成交量异常放大')
            
            if vwap_trend > 0.001:
                signal_strength *= 1.2
                reasons.append('VWAP上行趋势')
        
        if session_vwap > 0:
            session_deviation = (tick.price - session_vwap) / session_vwap
            if abs(session_deviation) > self.deviation_threshold * 1.5:
                signal_strength *= 0.8
                reasons.append(f'日内VWAP偏离{session_deviation*100:.2f}%')
        
        return {
            'action': action,
            'strength': signal_strength,
            'reason': ' | '.join(reasons) if reasons else '无信号',
            'metrics': {
                'vwap': round(vwap, 2),
                'session_vwap': round(session_vwap, 2) if session_vwap else None,
                'deviation': round(deviation * 100, 2),
                'vwap_trend': round(vwap_trend * 100, 4),
                'volume_spike': is_volume_spike
            }
        }
