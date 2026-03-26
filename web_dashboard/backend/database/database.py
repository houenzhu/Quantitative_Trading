import pymysql
import logging
from typing import Optional, List, Dict, Any
from datetime import datetime
from decimal import Decimal
from contextlib import contextmanager
import json

from database.models import (
    AccountState, Position, Order, Trade, 
    EquitySnapshot, StockPoolItem, StrategyConfig,
    SystemConfig, RiskLog, SignalLog
)

logger = logging.getLogger(__name__)


class DatabaseManager:
    def __init__(self, host: str = 'localhost', port: int = 3306,
                 user: str = 'root', password: str = '', database: str = 'quant_trading',
                 charset: str = 'utf8mb4'):
        self.config = {
            'host': host,
            'port': port,
            'user': user,
            'password': password,
            'database': database,
            'charset': charset,
            'autocommit': False
        }
        self._connection = None
    
    def get_connection(self):
        if self._connection is None or not self._connection.open:
            self._connection = pymysql.connect(**self.config)
        return self._connection
    
    @contextmanager
    def get_cursor(self, dictionary=True):
        conn = self.get_connection()
        cursor = conn.cursor(pymysql.cursors.DictCursor if dictionary else pymysql.cursors.Cursor)
        try:
            yield cursor
            conn.commit()
        except Exception as e:
            conn.rollback()
            logger.error(f"数据库操作失败: {e}")
            raise
        finally:
            cursor.close()
    
    def close(self):
        if self._connection and self._connection.open:
            self._connection.close()
            logger.info("数据库连接已关闭")
    
    def execute_sql_file(self, file_path: str):
        with open(file_path, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        statements = sql_content.split(';')
        with self.get_cursor(dictionary=False) as cursor:
            for statement in statements:
                statement = statement.strip()
                if statement and not statement.startswith('--'):
                    try:
                        cursor.execute(statement)
                    except Exception as e:
                        logger.warning(f"执行SQL语句失败: {statement[:50]}... 错误: {e}")


class AccountStateRepository:
    def __init__(self, db: DatabaseManager):
        self.db = db
    
    def save(self, state: AccountState) -> int:
        with self.db.get_cursor() as cursor:
            sql = '''
                INSERT INTO account_state (
                    timestamp, initial_capital, current_capital, total_equity,
                    cash, positions_value, total_pnl, realized_pnl, unrealized_pnl,
                    daily_pnl, daily_loss, trade_count, win_count, loss_count
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            '''
            cursor.execute(sql, (
                state.timestamp, state.initial_capital, state.current_capital,
                state.total_equity, state.cash, state.positions_value,
                state.total_pnl, state.realized_pnl, state.unrealized_pnl,
                state.daily_pnl, state.daily_loss, state.trade_count,
                state.win_count, state.loss_count
            ))
            return cursor.lastrowid
    
    def get_latest(self) -> Optional[AccountState]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT * FROM account_state ORDER BY timestamp DESC LIMIT 1')
            row = cursor.fetchone()
            if row:
                return AccountState.from_dict(row)
        return None
    
    def get_by_date_range(self, start_date: datetime, end_date: datetime) -> List[AccountState]:
        with self.db.get_cursor() as cursor:
            sql = 'SELECT * FROM account_state WHERE timestamp BETWEEN %s AND %s ORDER BY timestamp'
            cursor.execute(sql, (start_date, end_date))
            return [AccountState.from_dict(row) for row in cursor.fetchall()]


class PositionRepository:
    def __init__(self, db: DatabaseManager):
        self.db = db
    
    def save(self, position: Position) -> int:
        with self.db.get_cursor() as cursor:
            sql = '''
                INSERT INTO positions (
                    stock_code, stock_name, quantity, available_quantity,
                    avg_cost, current_price, market_value, unrealized_pnl,
                    unrealized_pnl_pct, opened_at, updated_at, closed_at, is_active
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    quantity = VALUES(quantity),
                    available_quantity = VALUES(available_quantity),
                    avg_cost = VALUES(avg_cost),
                    current_price = VALUES(current_price),
                    market_value = VALUES(market_value),
                    unrealized_pnl = VALUES(unrealized_pnl),
                    unrealized_pnl_pct = VALUES(unrealized_pnl_pct),
                    updated_at = VALUES(updated_at),
                    closed_at = VALUES(closed_at),
                    is_active = VALUES(is_active)
            '''
            cursor.execute(sql, (
                position.stock_code, position.stock_name, position.quantity,
                position.available_quantity, position.avg_cost, position.current_price,
                position.market_value, position.unrealized_pnl, position.unrealized_pnl_pct,
                position.opened_at, position.updated_at, position.closed_at,
                1 if position.is_active else 0
            ))
            return cursor.lastrowid
    
    def get_active_positions(self) -> List[Position]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT * FROM positions WHERE is_active = 1')
            return [Position.from_dict(row) for row in cursor.fetchall()]
    
    def get_by_stock_code(self, stock_code: str) -> Optional[Position]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT * FROM positions WHERE stock_code = %s AND is_active = 1', (stock_code,))
            row = cursor.fetchone()
            if row:
                return Position.from_dict(row)
        return None
    
    def deactivate(self, stock_code: str):
        with self.db.get_cursor() as cursor:
            sql = '''
                UPDATE positions SET is_active = 0, closed_at = %s, updated_at = %s
                WHERE stock_code = %s AND is_active = 1
            '''
            now = datetime.now()
            cursor.execute(sql, (now, now, stock_code))


class OrderRepository:
    def __init__(self, db: DatabaseManager):
        self.db = db
    
    def save(self, order: Order) -> int:
        with self.db.get_cursor() as cursor:
            sql = '''
                INSERT INTO orders (
                    order_id, stock_code, stock_name, side, order_type,
                    price, quantity, filled_quantity, avg_fill_price,
                    status, reason, created_at, submitted_at, filled_at,
                    cancelled_at, commission, slippage, error_message
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    filled_quantity = VALUES(filled_quantity),
                    avg_fill_price = VALUES(avg_fill_price),
                    status = VALUES(status),
                    submitted_at = VALUES(submitted_at),
                    filled_at = VALUES(filled_at),
                    cancelled_at = VALUES(cancelled_at),
                    commission = VALUES(commission),
                    slippage = VALUES(slippage),
                    error_message = VALUES(error_message)
            '''
            cursor.execute(sql, (
                order.order_id, order.stock_code, order.stock_name, order.side,
                order.order_type, order.price, order.quantity, order.filled_quantity,
                order.avg_fill_price, order.status, order.reason, order.created_at,
                order.submitted_at, order.filled_at, order.cancelled_at,
                order.commission, order.slippage, order.error_message
            ))
            return cursor.lastrowid
    
    def get_by_order_id(self, order_id: str) -> Optional[Order]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT * FROM orders WHERE order_id = %s', (order_id,))
            row = cursor.fetchone()
            if row:
                return Order.from_dict(row)
        return None
    
    def get_recent_orders(self, limit: int = 100) -> List[Order]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT * FROM orders ORDER BY created_at DESC LIMIT %s', (limit,))
            return [Order.from_dict(row) for row in cursor.fetchall()]


class TradeRepository:
    def __init__(self, db: DatabaseManager):
        self.db = db
    
    def save(self, trade: Trade) -> int:
        with self.db.get_cursor() as cursor:
            sql = '''
                INSERT INTO trades (
                    trade_id, order_id, stock_code, stock_name, side,
                    quantity, price, amount, commission, stamp_duty,
                    transfer_fee, slippage_cost, realized_pnl, traded_at, strategy_reason
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            '''
            cursor.execute(sql, (
                trade.trade_id, trade.order_id, trade.stock_code, trade.stock_name,
                trade.side, trade.quantity, trade.price, trade.amount,
                trade.commission, trade.stamp_duty, trade.transfer_fee,
                trade.slippage_cost, trade.realized_pnl, trade.traded_at, trade.strategy_reason
            ))
            return cursor.lastrowid
    
    def get_recent_trades(self, limit: int = 100) -> List[Trade]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT * FROM trades ORDER BY traded_at DESC LIMIT %s', (limit,))
            return [Trade.from_dict(row) for row in cursor.fetchall()]


class EquitySnapshotRepository:
    def __init__(self, db: DatabaseManager):
        self.db = db
    
    def save(self, snapshot: EquitySnapshot) -> int:
        with self.db.get_cursor() as cursor:
            sql = '''
                INSERT INTO equity_snapshots (
                    snapshot_date, timestamp, total_equity, cash, positions_value,
                    daily_pnl, daily_pnl_pct, cumulative_pnl, cumulative_pnl_pct,
                    drawdown, drawdown_pct, peak_equity, trade_count_today, position_count
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    timestamp = VALUES(timestamp),
                    total_equity = VALUES(total_equity),
                    cash = VALUES(cash),
                    positions_value = VALUES(positions_value),
                    daily_pnl = VALUES(daily_pnl),
                    daily_pnl_pct = VALUES(daily_pnl_pct),
                    cumulative_pnl = VALUES(cumulative_pnl),
                    cumulative_pnl_pct = VALUES(cumulative_pnl_pct),
                    drawdown = VALUES(drawdown),
                    drawdown_pct = VALUES(drawdown_pct),
                    peak_equity = VALUES(peak_equity),
                    trade_count_today = VALUES(trade_count_today),
                    position_count = VALUES(position_count)
            '''
            cursor.execute(sql, (
                snapshot.snapshot_date, snapshot.timestamp, snapshot.total_equity,
                snapshot.cash, snapshot.positions_value, snapshot.daily_pnl,
                snapshot.daily_pnl_pct, snapshot.cumulative_pnl, snapshot.cumulative_pnl_pct,
                snapshot.drawdown, snapshot.drawdown_pct, snapshot.peak_equity,
                snapshot.trade_count_today, snapshot.position_count
            ))
            return cursor.lastrowid
    
    def get_latest(self) -> Optional[EquitySnapshot]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT * FROM equity_snapshots ORDER BY snapshot_date DESC LIMIT 1')
            row = cursor.fetchone()
            if row:
                return EquitySnapshot.from_dict(row)
        return None
    
    def get_by_date_range(self, days: int = 30) -> List[EquitySnapshot]:
        with self.db.get_cursor() as cursor:
            sql = '''
                SELECT * FROM equity_snapshots 
                WHERE snapshot_date >= DATE_SUB(CURDATE(), INTERVAL %s DAY)
                ORDER BY snapshot_date ASC
            '''
            cursor.execute(sql, (days,))
            return [EquitySnapshot.from_dict(row) for row in cursor.fetchall()]


class StockPoolRepository:
    def __init__(self, db: DatabaseManager):
        self.db = db
    
    def save_all(self, stock_pool: Dict[str, str]):
        with self.db.get_cursor() as cursor:
            cursor.execute('UPDATE stock_pool SET is_active = 0')
            for code, name in stock_pool.items():
                sql = '''
                    INSERT INTO stock_pool (stock_code, stock_name, added_at, is_active)
                    VALUES (%s, %s, %s, 1)
                    ON DUPLICATE KEY UPDATE
                        stock_name = VALUES(stock_name),
                        added_at = VALUES(added_at),
                        is_active = 1
                '''
                cursor.execute(sql, (code, name, datetime.now()))
    
    def get_active(self) -> Dict[str, str]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT stock_code, stock_name FROM stock_pool WHERE is_active = 1')
            return {row['stock_code']: row['stock_name'] for row in cursor.fetchall()}
    
    def add_stock(self, stock_code: str, stock_name: str):
        with self.db.get_cursor() as cursor:
            sql = '''
                INSERT INTO stock_pool (stock_code, stock_name, added_at, is_active)
                VALUES (%s, %s, %s, 1)
                ON DUPLICATE KEY UPDATE is_active = 1, stock_name = VALUES(stock_name)
            '''
            cursor.execute(sql, (stock_code, stock_name, datetime.now()))
    
    def remove_stock(self, stock_code: str):
        with self.db.get_cursor() as cursor:
            cursor.execute('UPDATE stock_pool SET is_active = 0, removed_at = %s WHERE stock_code = %s',
                          (datetime.now(), stock_code))


class SystemConfigRepository:
    def __init__(self, db: DatabaseManager):
        self.db = db
    
    def get(self, key: str, default: str = None) -> Optional[str]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT config_value FROM system_config WHERE config_key = %s', (key,))
            row = cursor.fetchone()
            return row['config_value'] if row else default
    
    def get_float(self, key: str, default: float = 0.0) -> float:
        value = self.get(key)
        return float(value) if value else default
    
    def get_int(self, key: str, default: int = 0) -> int:
        value = self.get(key)
        return int(value) if value else default
    
    def set(self, key: str, value: str):
        with self.db.get_cursor() as cursor:
            sql = '''
                INSERT INTO system_config (config_key, config_value, updated_at)
                VALUES (%s, %s, %s)
                ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = VALUES(updated_at)
            '''
            cursor.execute(sql, (key, value, datetime.now()))
    
    def get_all(self) -> Dict[str, str]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT config_key, config_value FROM system_config')
            return {row['config_key']: row['config_value'] for row in cursor.fetchall()}


class RiskLogRepository:
    def __init__(self, db: DatabaseManager):
        self.db = db
    
    def save(self, log: RiskLog) -> int:
        with self.db.get_cursor() as cursor:
            sql = '''
                INSERT INTO risk_logs (timestamp, event_type, severity, stock_code, message, details, action_taken)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
            '''
            cursor.execute(sql, (
                log.timestamp, log.event_type, log.severity,
                log.stock_code, log.message, log.details, log.action_taken
            ))
            return cursor.lastrowid
    
    def get_recent(self, limit: int = 100) -> List[RiskLog]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT * FROM risk_logs ORDER BY timestamp DESC LIMIT %s', (limit,))
            return [RiskLog.from_dict(row) for row in cursor.fetchall()]


class SignalLogRepository:
    def __init__(self, db: DatabaseManager):
        self.db = db
    
    def save(self, log: SignalLog) -> int:
        with self.db.get_cursor() as cursor:
            sql = '''
                INSERT INTO signal_logs (
                    timestamp, stock_code, stock_name, strategy_name, action,
                    strength, reason, metrics, executed
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            '''
            cursor.execute(sql, (
                log.timestamp, log.stock_code, log.stock_name, log.strategy_name,
                log.action, log.strength, log.reason, log.metrics, 1 if log.executed else 0
            ))
            return cursor.lastrowid
    
    def get_recent(self, limit: int = 100) -> List[SignalLog]:
        with self.db.get_cursor() as cursor:
            cursor.execute('SELECT * FROM signal_logs ORDER BY timestamp DESC LIMIT %s', (limit,))
            return [SignalLog.from_dict(row) for row in cursor.fetchall()]
