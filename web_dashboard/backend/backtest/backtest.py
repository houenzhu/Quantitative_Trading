from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional, Callable
import logging
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick, Order, OrderSide, OrderType, OrderStatus, Position

logger = logging.getLogger(__name__)


@dataclass
class BacktestConfig:
    initial_capital: float = 1000000.0
    commission_rate: float = 0.0003
    slippage_rate: float = 0.0001
    stamp_duty: float = 0.001
    min_commission: float = 5.0
    max_position_pct: float = 0.2
    max_daily_loss_pct: float = 0.05
    risk_free_rate: float = 0.03


@dataclass
class TradeRecord:
    timestamp: datetime
    stock_code: str
    stock_name: str
    side: str
    quantity: int
    price: float
    commission: float
    slippage: float
    pnl: float = 0.0


@dataclass
class BacktestResult:
    start_date: datetime
    end_date: datetime
    initial_capital: float
    final_capital: float
    total_return: float
    annual_return: float
    max_drawdown: float
    sharpe_ratio: float
    win_rate: float
    total_trades: int
    winning_trades: int
    losing_trades: int
    avg_profit: float
    avg_loss: float
    profit_factor: float
    trades: List[TradeRecord] = field(default_factory=list)
    equity_curve: List[Dict] = field(default_factory=list)
    daily_returns: List[float] = field(default_factory=list)


class BacktestEngine:
    def __init__(self, config: BacktestConfig = None):
        self.config = config or BacktestConfig()
        self.capital = self.config.initial_capital
        self.positions: Dict[str, Position] = {}
        self.trades: List[TradeRecord] = []
        self.equity_curve: List[Dict] = []
        self.daily_pnl: List[float] = []
        self.daily_loss = 0.0
        
    def reset(self):
        self.capital = self.config.initial_capital
        self.positions = {}
        self.trades = []
        self.equity_curve = []
        self.daily_pnl = []
        self.daily_loss = 0.0
    
    def calculate_commission(self, amount: float) -> float:
        commission = amount * self.config.commission_rate
        return max(commission, self.config.min_commission)
    
    def calculate_slippage(self, price: float, side: OrderSide) -> float:
        if side == OrderSide.BUY:
            return price * self.config.slippage_rate
        else:
            return -price * self.config.slippage_rate
    
    def calculate_stamp_duty(self, amount: float, side: OrderSide) -> float:
        if side == OrderSide.SELL:
            return amount * self.config.stamp_duty
        return 0.0
    
    def execute_trade(self, tick: Tick, side: OrderSide, quantity: int, reason: str = "") -> Optional[TradeRecord]:
        if quantity <= 0:
            return None
        
        if side == OrderSide.BUY:
            required_capital = tick.price * quantity * (1 + self.config.commission_rate + self.config.slippage_rate)
            if required_capital > self.capital:
                logger.debug(f"资金不足: 需要{required_capital:.2f}, 可用{self.capital:.2f}")
                return None
            
            position_value = self.positions.get(tick.stock_code, Position(tick.stock_code, tick.stock_name, 0, 0, 0, 0, 0, 0)).market_value
            if position_value + tick.price * quantity > self.config.initial_capital * self.config.max_position_pct:
                logger.debug(f"超过仓位限制: {self.config.max_position_pct*100}%")
                return None
        
        exec_price = tick.price + self.calculate_slippage(tick.price, side)
        amount = exec_price * quantity
        commission = self.calculate_commission(amount)
        stamp_duty = self.calculate_stamp_duty(amount, side)
        total_cost = amount + commission + stamp_duty
        
        pnl = 0.0
        if side == OrderSide.BUY:
            self.capital -= total_cost
            if tick.stock_code in self.positions:
                pos = self.positions[tick.stock_code]
                total_cost_basis = pos.avg_price * pos.quantity + exec_price * quantity
                pos.quantity += quantity
                pos.avg_price = total_cost_basis / pos.quantity
            else:
                self.positions[tick.stock_code] = Position(
                    stock_code=tick.stock_code,
                    stock_name=tick.stock_name,
                    quantity=quantity,
                    avg_price=exec_price,
                    current_price=exec_price,
                    market_value=amount,
                    profit_loss=0,
                    profit_loss_pct=0
                )
        else:
            if tick.stock_code not in self.positions:
                return None
            pos = self.positions[tick.stock_code]
            if pos.quantity < quantity:
                quantity = pos.quantity
            
            sell_amount = exec_price * quantity
            cost_basis = pos.avg_price * quantity
            pnl = sell_amount - cost_basis - commission - stamp_duty
            
            self.capital += sell_amount - commission - stamp_duty
            pos.quantity -= quantity
            
            if pos.quantity == 0:
                del self.positions[tick.stock_code]
        
        trade = TradeRecord(
            timestamp=tick.timestamp,
            stock_code=tick.stock_code,
            stock_name=tick.stock_name,
            side=side.value,
            quantity=quantity,
            price=exec_price,
            commission=commission,
            slippage=abs(exec_price - tick.price) * quantity
        )
        
        if side == OrderSide.SELL:
            trade.pnl = pnl
            self.daily_pnl.append(pnl)
            if pnl < 0:
                self.daily_loss += abs(pnl)
        
        self.trades.append(trade)
        return trade
    
    def update_positions(self, current_prices: Dict[str, float]):
        for code, pos in self.positions.items():
            if code in current_prices:
                pos.current_price = current_prices[code]
                pos.market_value = pos.quantity * current_prices[code]
                pos.profit_loss = pos.market_value - pos.quantity * pos.avg_price
                pos.profit_loss_pct = (pos.current_price / pos.avg_price - 1) * 100
    
    def record_equity(self, timestamp: datetime, current_prices: Dict[str, float]):
        self.update_positions(current_prices)
        
        positions_value = sum(pos.market_value for pos in self.positions.values())
        total_equity = self.capital + positions_value
        
        self.equity_curve.append({
            'timestamp': timestamp,
            'capital': self.capital,
            'positions_value': positions_value,
            'total_equity': total_equity
        })
    
    def calculate_max_drawdown(self) -> float:
        if len(self.equity_curve) < 2:
            return 0.0
        
        peak = self.equity_curve[0]['total_equity']
        max_dd = 0.0
        
        for record in self.equity_curve:
            equity = record['total_equity']
            if equity > peak:
                peak = equity
            drawdown = (peak - equity) / peak
            if drawdown > max_dd:
                max_dd = drawdown
        
        return max_dd
    
    def calculate_sharpe_ratio(self) -> float:
        if len(self.daily_pnl) < 2:
            return 0.0
        
        import statistics
        avg_return = statistics.mean(self.daily_pnl)
        std_return = statistics.stdev(self.daily_pnl)
        
        if std_return == 0:
            return 0.0
        
        risk_free_daily = self.config.risk_free_rate / 252
        excess_return = avg_return - risk_free_daily * self.config.initial_capital
        
        return (excess_return / std_return) * (252 ** 0.5)
    
    def calculate_statistics(self) -> BacktestResult:
        if not self.trades:
            return BacktestResult(
                start_date=datetime.now(),
                end_date=datetime.now(),
                initial_capital=self.config.initial_capital,
                final_capital=self.capital,
                total_return=0,
                annual_return=0,
                max_drawdown=0,
                sharpe_ratio=0,
                win_rate=0,
                total_trades=0,
                winning_trades=0,
                losing_trades=0,
                avg_profit=0,
                avg_loss=0,
                profit_factor=0
            )
        
        sell_trades = [t for t in self.trades if '卖出' in t.side]
        winning = [t for t in sell_trades if t.pnl > 0]
        losing = [t for t in sell_trades if t.pnl < 0]
        
        total_pnl = sum(t.pnl for t in sell_trades)
        gross_profit = sum(t.pnl for t in winning)
        gross_loss = abs(sum(t.pnl for t in losing))
        
        profit_factor = gross_profit / gross_loss if gross_loss > 0 else float('inf')
        
        start_date = self.trades[0].timestamp
        end_date = self.trades[-1].timestamp
        
        final_equity = self.equity_curve[-1]['total_equity'] if self.equity_curve else self.capital
        total_return = (final_equity - self.config.initial_capital) / self.config.initial_capital
        
        days = (end_date - start_date).days + 1
        annual_return = ((1 + total_return) ** (252 / max(days, 1)) - 1) if days > 0 else 0
        
        return BacktestResult(
            start_date=start_date,
            end_date=end_date,
            initial_capital=self.config.initial_capital,
            final_capital=final_equity,
            total_return=total_return * 100,
            annual_return=annual_return * 100,
            max_drawdown=self.calculate_max_drawdown() * 100,
            sharpe_ratio=self.calculate_sharpe_ratio(),
            win_rate=len(winning) / len(sell_trades) * 100 if sell_trades else 0,
            total_trades=len(self.trades),
            winning_trades=len(winning),
            losing_trades=len(losing),
            avg_profit=sum(t.pnl for t in winning) / len(winning) if winning else 0,
            avg_loss=sum(t.pnl for t in losing) / len(losing) if losing else 0,
            profit_factor=profit_factor,
            trades=self.trades,
            equity_curve=self.equity_curve,
            daily_returns=self.daily_pnl
        )
    
    def run_backtest(
        self,
        historical_data: Dict[str, List[Tick]],
        strategy_func: Callable[[Tick, List[Tick], Dict], Optional[Dict]],
        progress_callback: Optional[Callable[[int, int], None]] = None
    ) -> BacktestResult:
        self.reset()
        
        all_ticks = []
        for stock_code, ticks in historical_data.items():
            for tick in ticks:
                all_ticks.append((tick.timestamp, stock_code, tick))
        
        all_ticks.sort(key=lambda x: x[0])
        
        total_ticks = len(all_ticks)
        current_prices: Dict[str, float] = {}
        
        history_by_stock: Dict[str, List[Tick]] = {code: [] for code in historical_data}
        
        for i, (timestamp, stock_code, tick) in enumerate(all_ticks):
            history_by_stock[stock_code].append(tick)
            if len(history_by_stock[stock_code]) > 500:
                history_by_stock[stock_code] = history_by_stock[stock_code][-500:]
            
            current_prices[stock_code] = tick.price
            
            signal = strategy_func(tick, history_by_stock[stock_code], {
                'capital': self.capital,
                'positions': self.positions,
                'config': self.config
            })
            
            if signal and signal.get('action') != 'HOLD':
                side = OrderSide.BUY if signal['action'] == 'BUY' else OrderSide.SELL
                quantity = signal.get('quantity', 0)
                
                if quantity > 0:
                    self.execute_trade(tick, side, quantity, signal.get('reason', ''))
            
            self.record_equity(timestamp, current_prices)
            
            if progress_callback and i % 100 == 0:
                progress_callback(i, total_ticks)
        
        return self.calculate_statistics()
