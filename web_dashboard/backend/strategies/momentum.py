from typing import Dict, List
from collections import deque
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick
from .base import TradingStrategy


class MomentumStrategy(TradingStrategy):
    def __init__(
        self, 
        momentum_threshold: float = 2.0,
        rsi_period: int = 14,
        rsi_overbought: float = 70,
        rsi_oversold: float = 30,
        macd_fast: int = 12,
        macd_slow: int = 26,
        macd_signal: int = 9
    ):
        super().__init__("动量策略")
        self.momentum_threshold = momentum_threshold
        self.rsi_period = rsi_period
        self.rsi_overbought = rsi_overbought
        self.rsi_oversold = rsi_oversold
        self.macd_fast = macd_fast
        self.macd_slow = macd_slow
        self.macd_signal = macd_signal
        
        self.price_history: deque = deque(maxlen=200)
        self.gain_history: deque = deque(maxlen=rsi_period)
        self.loss_history: deque = deque(maxlen=rsi_period)
        self.ema_fast: float = 0
        self.ema_slow: float = 0
        self.macd_line: deque = deque(maxlen=macd_signal)
    
    def calculate_rsi(self, history: List[Tick]) -> float:
        if len(history) < self.rsi_period + 1:
            return 50
        
        gains = []
        losses = []
        
        prices = [t.price for t in history[-(self.rsi_period + 1):]]
        
        for i in range(1, len(prices)):
            change = prices[i] - prices[i-1]
            if change > 0:
                gains.append(change)
                losses.append(0)
            else:
                gains.append(0)
                losses.append(abs(change))
        
        if not gains and not losses:
            return 50
        
        avg_gain = sum(gains) / len(gains) if gains else 0
        avg_loss = sum(losses) / len(losses) if losses else 0
        
        if avg_loss == 0:
            return 100
        
        rs = avg_gain / avg_loss
        rsi = 100 - (100 / (1 + rs))
        
        return rsi
    
    def calculate_ema(self, price: float, prev_ema: float, period: int) -> float:
        if prev_ema == 0:
            return price
        multiplier = 2 / (period + 1)
        return (price - prev_ema) * multiplier + prev_ema
    
    def calculate_macd(self, history: List[Tick]) -> Dict:
        if len(history) < self.macd_slow:
            return {'macd': 0, 'signal': 0, 'histogram': 0}
        
        prices = [t.price for t in history]
        
        if self.ema_fast == 0:
            self.ema_fast = sum(prices[:self.macd_fast]) / self.macd_fast
            self.ema_slow = sum(prices[:self.macd_slow]) / self.macd_slow
        
        for price in prices[-50:]:
            self.ema_fast = self.calculate_ema(price, self.ema_fast, self.macd_fast)
            self.ema_slow = self.calculate_ema(price, self.ema_slow, self.macd_slow)
        
        macd_value = self.ema_fast - self.ema_slow
        self.macd_line.append(macd_value)
        
        signal = sum(self.macd_line) / len(self.macd_line) if self.macd_line else 0
        histogram = macd_value - signal
        
        return {
            'macd': macd_value,
            'signal': signal,
            'histogram': histogram
        }
    
    def calculate_momentum(self, history: List[Tick], period: int = 10) -> float:
        if len(history) < period:
            return 0
        
        current_price = history[-1].price
        past_price = history[-period].price
        
        return (current_price - past_price) / past_price * 100
    
    def calculate_roc(self, history: List[Tick], period: int = 12) -> float:
        if len(history) < period + 1:
            return 0
        
        current_price = history[-1].price
        past_price = history[-(period + 1)].price
        
        return ((current_price - past_price) / past_price) * 100
    
    def detect_divergence(self, history: List[Tick], rsi: float) -> str:
        if len(history) < 20:
            return 'none'
        
        prices = [t.price for t in history[-20:]]
        
        price_higher_high = prices[-1] > max(prices[-10:-1])
        price_lower_low = prices[-1] < min(prices[-10:-1])
        
        if price_higher_high and rsi < 60:
            return 'bearish'
        elif price_lower_low and rsi > 40:
            return 'bullish'
        
        return 'none'
    
    def analyze(self, tick: Tick, history: List[Tick]) -> Dict:
        if len(history) < 20:
            return {'action': 'HOLD', 'strength': 0, 'reason': '历史数据不足'}
        
        self.price_history.append(tick.price)
        
        rsi = self.calculate_rsi(history)
        macd_data = self.calculate_macd(history)
        
        short_momentum = tick.change_percent
        medium_momentum = self.calculate_momentum(history, 10)
        long_momentum = self.calculate_momentum(history, 20)
        
        roc = self.calculate_roc(history)
        
        divergence = self.detect_divergence(history, rsi)
        
        signal_strength = 0
        action = 'HOLD'
        reasons = []
        
        buy_signals = 0
        sell_signals = 0
        
        if rsi < self.rsi_oversold:
            buy_signals += 2
            reasons.append(f'RSI超卖({rsi:.1f})')
        elif rsi > self.rsi_overbought:
            sell_signals += 2
            reasons.append(f'RSI超买({rsi:.1f})')
        
        if macd_data['histogram'] > 0 and macd_data['macd'] > macd_data['signal']:
            buy_signals += 1
            reasons.append('MACD金叉')
        elif macd_data['histogram'] < 0 and macd_data['macd'] < macd_data['signal']:
            sell_signals += 1
            reasons.append('MACD死叉')
        
        if short_momentum > self.momentum_threshold and medium_momentum > 0:
            buy_signals += 1
            reasons.append(f'短期动量强劲({short_momentum:.2f}%)')
        elif short_momentum < -self.momentum_threshold and medium_momentum < 0:
            sell_signals += 1
            reasons.append(f'短期动量衰竭({short_momentum:.2f}%)')
        
        if divergence == 'bullish':
            buy_signals += 2
            reasons.append('底背离信号')
        elif divergence == 'bearish':
            sell_signals += 2
            reasons.append('顶背离信号')
        
        if roc > 5:
            buy_signals += 1
            reasons.append(f'ROC强势({roc:.2f}%)')
        elif roc < -5:
            sell_signals += 1
            reasons.append(f'ROC弱势({roc:.2f}%)')
        
        if long_momentum > 0 and medium_momentum > 0:
            buy_signals += 1
            reasons.append('多周期动量向上')
        elif long_momentum < 0 and medium_momentum < 0:
            sell_signals += 1
            reasons.append('多周期动量向下')
        
        total_signals = buy_signals + sell_signals
        if total_signals > 0:
            if buy_signals > sell_signals and buy_signals >= 2:
                action = 'BUY'
                signal_strength = min(0.9, buy_signals / 5)
            elif sell_signals > buy_signals and sell_signals >= 2:
                action = 'SELL'
                signal_strength = min(0.9, sell_signals / 5)
        
        return {
            'action': action,
            'strength': signal_strength,
            'reason': ' | '.join(reasons) if reasons else '动量正常',
            'metrics': {
                'rsi': round(rsi, 2),
                'macd': round(macd_data['macd'], 4),
                'macd_signal': round(macd_data['signal'], 4),
                'macd_histogram': round(macd_data['histogram'], 4),
                'momentum_short': round(short_momentum, 2),
                'momentum_medium': round(medium_momentum, 2),
                'momentum_long': round(long_momentum, 2),
                'roc': round(roc, 2),
                'divergence': divergence,
                'buy_signals': buy_signals,
                'sell_signals': sell_signals
            }
        }
