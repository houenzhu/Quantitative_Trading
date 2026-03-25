from typing import Dict, List
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Order, Position, OrderSide


class PortfolioManager:
    def __init__(self, initial_capital: float):
        self.initial_capital = initial_capital
        self.capital = initial_capital
        self.positions: Dict[str, Position] = {}
        self.trades = []
        self.equity_curve = []
    
    def update_position(self, order: Order):
        if order.side == OrderSide.BUY:
            if order.stock_code in self.positions:
                pos = self.positions[order.stock_code]
                total_cost = pos.avg_price * pos.quantity + order.executed_price * order.quantity
                new_quantity = pos.quantity + order.quantity
                pos.avg_price = total_cost / new_quantity
                pos.quantity = new_quantity
            else:
                self.positions[order.stock_code] = Position(
                    stock_code=order.stock_code,
                    stock_name=order.stock_name,
                    quantity=order.quantity,
                    avg_price=order.executed_price,
                    current_price=order.executed_price,
                    market_value=order.executed_price * order.quantity,
                    profit_loss=0,
                    profit_loss_pct=0
                )
            
            self.capital -= order.executed_price * order.quantity
            
        elif order.side == OrderSide.SELL:
            if order.stock_code in self.positions:
                pos = self.positions[order.stock_code]
                pos.quantity -= order.quantity
                self.capital += order.executed_price * order.quantity
                
                if pos.quantity == 0:
                    del self.positions[order.stock_code]
        
        self.trades.append(order)
    
    def update_market_value(self, current_prices: Dict[str, float]):
        total_value = self.capital
        
        for code, pos in self.positions.items():
            if code in current_prices:
                pos.current_price = current_prices[code]
                pos.market_value = pos.quantity * current_prices[code]
                pos.profit_loss = pos.market_value - pos.quantity * pos.avg_price
                pos.profit_loss_pct = (pos.current_price / pos.avg_price - 1) * 100
                total_value += pos.market_value
        
        self.equity_curve.append({
            'timestamp': total_value,
            'total_value': total_value,
            'capital': self.capital,
            'positions_value': total_value - self.capital
        })
        
        return total_value
    
    def get_total_value(self) -> float:
        if self.equity_curve:
            return self.equity_curve[-1]['total_value']
        return self.capital
    
    def get_statistics(self) -> Dict:
        if not self.equity_curve:
            return {}
        
        values = [e['total_value'] for e in self.equity_curve]
        
        total_return = (values[-1] - self.initial_capital) / self.initial_capital * 100
        
        sell_trades = [t for t in self.trades if t.side == OrderSide.SELL]
        winning_trades = 0
        for trade in sell_trades:
            winning_trades += 1
        
        win_rate = winning_trades / len(sell_trades) * 100 if sell_trades else 0
        
        return {
            '总资产': f"¥{values[-1]:,.2f}",
            '总收益率': f"{total_return:.2f}%",
            '持仓数量': len(self.positions),
            '交易次数': len(self.trades),
            '胜率': f"{win_rate:.2f}%",
            '最大回撤': self.calculate_max_drawdown(values)
        }
    
    def calculate_max_drawdown(self, values: List[float]) -> str:
        peak = values[0]
        max_drawdown = 0
        
        for value in values:
            if value > peak:
                peak = value
            drawdown = (peak - value) / peak * 100
            if drawdown > max_drawdown:
                max_drawdown = drawdown
        
        return f"{max_drawdown:.2f}%"
