from typing import Dict, List, Tuple
from datetime import datetime, timedelta
from collections import deque
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
        
        self.pnl_history: deque = deque(maxlen=252)
        self.position_history: deque = deque(maxlen=100)
        self.trade_history: deque = deque(maxlen=1000)
        self.peak_equity = self.initial_capital
        self.current_equity = self.initial_capital
        
        self.var_confidence = config.get('var_confidence', 0.95)
        self.max_drawdown_limit = config.get('max_drawdown_limit', 0.15)
        self.max_correlation = config.get('max_correlation', 0.7)
    
    def can_trade(self, position: Position, side: OrderSide) -> Tuple[bool, str]:
        if self.daily_loss > self.config.get('max_daily_loss', 0.05) * self.initial_capital:
            return False, "超过单日亏损限制"
        
        max_position_pct = self.config.get('max_position_pct', 0.2)
        if side == OrderSide.BUY:
            position_value = position.market_value if position else 0
            if position_value > self.initial_capital * max_position_pct:
                return False, f"超过单只股票仓位限制 {max_position_pct*100}%"
        
        current_drawdown = self.calculate_current_drawdown()
        if current_drawdown > self.max_drawdown_limit:
            return False, f"超过最大回撤限制 {self.max_drawdown_limit*100}%"
        
        var = self.calculate_var()
        if var > self.initial_capital * 0.1:
            return False, f"VaR风险过高: {var:.2f}"
        
        return True, "通过风控检查"
    
    def update_pnl(self, pnl: float):
        self.daily_pnl += pnl
        if pnl < 0:
            self.daily_loss += abs(pnl)
        
        self.pnl_history.append(pnl)
    
    def update_equity(self, equity: float):
        self.current_equity = equity
        if equity > self.peak_equity:
            self.peak_equity = equity
    
    def reset_daily(self):
        self.daily_loss = 0
        self.daily_pnl = 0
    
    def calculate_current_drawdown(self) -> float:
        if self.peak_equity == 0:
            return 0
        return (self.peak_equity - self.current_equity) / self.peak_equity
    
    def calculate_max_drawdown(self) -> float:
        if len(self.position_history) < 2:
            return 0
        
        peak = self.position_history[0]
        max_dd = 0
        
        for equity in self.position_history:
            if equity > peak:
                peak = equity
            dd = (peak - equity) / peak if peak > 0 else 0
            if dd > max_dd:
                max_dd = dd
        
        return max_dd
    
    def calculate_var(self, confidence: float = None) -> float:
        if confidence is None:
            confidence = self.var_confidence
        
        if len(self.pnl_history) < 20:
            return 0
        
        pnls = sorted(list(self.pnl_history))
        index = int((1 - confidence) * len(pnls))
        var = abs(pnls[index])
        
        return var
    
    def calculate_cvar(self, confidence: float = None) -> float:
        if confidence is None:
            confidence = self.var_confidence
        
        if len(self.pnl_history) < 20:
            return 0
        
        pnls = sorted(list(self.pnl_history))
        index = int((1 - confidence) * len(pnls))
        
        tail_losses = pnls[:index + 1]
        cvar = abs(sum(tail_losses) / len(tail_losses)) if tail_losses else 0
        
        return cvar
    
    def calculate_sharpe_ratio(self, risk_free_rate: float = 0.03) -> float:
        if len(self.pnl_history) < 20:
            return 0
        
        import statistics
        avg_pnl = statistics.mean(self.pnl_history)
        std_pnl = statistics.stdev(self.pnl_history)
        
        if std_pnl == 0:
            return 0
        
        daily_rf = risk_free_rate / 252
        excess_return = avg_pnl - daily_rf * self.initial_capital
        
        return (excess_return / std_pnl) * (252 ** 0.5)
    
    def calculate_sortino_ratio(self, risk_free_rate: float = 0.03) -> float:
        if len(self.pnl_history) < 20:
            return 0
        
        import statistics
        pnls = list(self.pnl_history)
        avg_pnl = statistics.mean(pnls)
        
        negative_returns = [p for p in pnls if p < 0]
        if not negative_returns:
            return float('inf')
        
        downside_std = statistics.stdev(negative_returns)
        
        if downside_std == 0:
            return 0
        
        daily_rf = risk_free_rate / 252
        excess_return = avg_pnl - daily_rf * self.initial_capital
        
        return (excess_return / downside_std) * (252 ** 0.5)
    
    def calculate_win_rate(self) -> float:
        if not self.trade_history:
            return 0
        
        wins = sum(1 for t in self.trade_history if t.get('pnl', 0) > 0)
        return wins / len(self.trade_history) * 100
    
    def calculate_profit_factor(self) -> float:
        if not self.trade_history:
            return 0
        
        gross_profit = sum(t.get('pnl', 0) for t in self.trade_history if t.get('pnl', 0) > 0)
        gross_loss = abs(sum(t.get('pnl', 0) for t in self.trade_history if t.get('pnl', 0) < 0))
        
        if gross_loss == 0:
            return float('inf') if gross_profit > 0 else 0
        
        return gross_profit / gross_loss
    
    def calculate_volatility(self, annualize: bool = True) -> float:
        if len(self.pnl_history) < 20:
            return 0
        
        import statistics
        std = statistics.stdev(self.pnl_history)
        
        if annualize:
            std = std * (252 ** 0.5)
        
        return std
    
    def record_trade(self, trade_info: Dict):
        self.trade_history.append(trade_info)
        if 'pnl' in trade_info:
            self.update_pnl(trade_info['pnl'])
    
    def get_risk_metrics(self) -> Dict:
        return {
            'current_equity': round(self.current_equity, 2),
            'peak_equity': round(self.peak_equity, 2),
            'current_drawdown': round(self.calculate_current_drawdown() * 100, 2),
            'max_drawdown': round(self.calculate_max_drawdown() * 100, 2),
            'var_95': round(self.calculate_var(0.95), 2),
            'cvar_95': round(self.calculate_cvar(0.95), 2),
            'sharpe_ratio': round(self.calculate_sharpe_ratio(), 3),
            'sortino_ratio': round(self.calculate_sortino_ratio(), 3),
            'win_rate': round(self.calculate_win_rate(), 2),
            'profit_factor': round(self.calculate_profit_factor(), 2),
            'volatility': round(self.calculate_volatility(), 2),
            'daily_pnl': round(self.daily_pnl, 2),
            'daily_loss': round(self.daily_loss, 2),
            'trade_count': len(self.trade_history)
        }
    
    def check_position_concentration(self, positions: Dict[str, Position]) -> Tuple[bool, str]:
        if not positions:
            return True, "无持仓"
        
        total_value = sum(p.market_value for p in positions.values())
        if total_value == 0:
            return True, "无持仓"
        
        for code, pos in positions.items():
            weight = pos.market_value / total_value
            if weight > 0.5:
                return False, f"股票 {pos.stock_name} 权重过高 ({weight*100:.1f}%)"
        
        return True, "持仓分散度合理"
    
    def calculate_position_risk(self, positions: Dict[str, Position]) -> float:
        if not positions:
            return 0
        
        total_risk = 0
        for pos in positions.values():
            position_risk = pos.market_value * 0.02
            total_risk += position_risk
        
        return total_risk
    
    def get_position_limit(self, stock_code: str = None) -> float:
        base_limit = self.initial_capital * self.config.get('max_position_pct', 0.2)
        
        current_dd = self.calculate_current_drawdown()
        if current_dd > 0.05:
            base_limit *= 0.5
        elif current_dd > 0.03:
            base_limit *= 0.75
        
        return base_limit
