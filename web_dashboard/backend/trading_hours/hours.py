from datetime import datetime, time, timedelta
from typing import Optional, Tuple
import logging

logger = logging.getLogger(__name__)


class TradingHours:
    MORNING_START = time(9, 30)
    MORNING_END = time(11, 30)
    AFTERNOON_START = time(13, 0)
    AFTERNOON_END = time(15, 0)
    
    TRADING_SESSIONS = [
        (MORNING_START, MORNING_END),
        (AFTERNOON_START, AFTERNOON_END)
    ]
    
    HOLIDAYS_2024 = [
        "2024-01-01", "2024-02-10", "2024-02-11", "2024-02-12",
        "2024-02-13", "2024-02-14", "2024-02-15", "2024-02-16", "2024-02-17",
        "2024-04-04", "2024-04-05", "2024-04-06",
        "2024-05-01", "2024-05-02", "2024-05-03", "2024-05-04", "2024-05-05",
        "2024-06-08", "2024-06-09", "2024-06-10",
        "2024-09-15", "2024-09-16", "2024-09-17",
        "2024-10-01", "2024-10-02", "2024-10-03", "2024-10-04", "2024-10-05",
        "2024-10-06", "2024-10-07",
    ]
    
    HOLIDAYS_2025 = [
        "2025-01-01",
        "2025-01-28", "2025-01-29", "2025-01-30", "2025-01-31",
        "2025-02-01", "2025-02-02", "2025-02-03", "2025-02-04",
        "2025-04-04", "2025-04-05", "2025-04-06",
        "2025-05-01", "2025-05-02", "2025-05-03", "2025-05-04", "2025-05-05",
        "2025-05-31", "2025-06-01", "2025-06-02",
        "2025-10-01", "2025-10-02", "2025-10-03", "2025-10-04",
        "2025-10-05", "2025-10-06", "2025-10-07", "2025-10-08",
    ]
    
    def __init__(self, custom_holidays: Optional[list] = None):
        self.holidays = set(self.HOLIDAYS_2024 + self.HOLIDAYS_2025)
        if custom_holidays:
            self.holidays.update(custom_holidays)
    
    def is_holiday(self, dt: datetime) -> bool:
        date_str = dt.strftime("%Y-%m-%d")
        return date_str in self.holidays
    
    def is_weekend(self, dt: datetime) -> bool:
        return dt.weekday() >= 5
    
    def is_trading_day(self, dt: datetime) -> bool:
        if self.is_weekend(dt):
            return False
        if self.is_holiday(dt):
            return False
        return True
    
    def is_trading_time(self, dt: Optional[datetime] = None) -> bool:
        if dt is None:
            dt = datetime.now()
        
        if not self.is_trading_day(dt):
            return False
        
        current_time = dt.time()
        
        for start, end in self.TRADING_SESSIONS:
            if start <= current_time <= end:
                return True
        
        return False
    
    def get_current_session(self, dt: Optional[datetime] = None) -> Optional[Tuple[time, time]]:
        if dt is None:
            dt = datetime.now()
        
        if not self.is_trading_day(dt):
            return None
        
        current_time = dt.time()
        
        for session in self.TRADING_SESSIONS:
            if session[0] <= current_time <= session[1]:
                return session
        
        return None
    
    def get_session_name(self, dt: Optional[datetime] = None) -> Optional[str]:
        session = self.get_current_session(dt)
        if session is None:
            return None
        
        if session == (self.MORNING_START, self.MORNING_END):
            return "上午盘"
        elif session == (self.AFTERNOON_START, self.AFTERNOON_END):
            return "下午盘"
        
        return None
    
    def get_next_trading_time(self, dt: Optional[datetime] = None) -> Optional[datetime]:
        if dt is None:
            dt = datetime.now()
        
        current_time = dt.time()
        
        if self.is_trading_day(dt):
            if current_time < self.MORNING_START:
                return datetime.combine(dt.date(), self.MORNING_START)
            elif self.MORNING_END < current_time < self.AFTERNOON_START:
                return datetime.combine(dt.date(), self.AFTERNOON_START)
            elif current_time > self.AFTERNOON_END:
                return self._get_next_day_morning(dt)
        else:
            return self._get_next_day_morning(dt)
        
        return None
    
    def _get_next_day_morning(self, dt: datetime) -> datetime:
        next_day = dt + timedelta(days=1)
        
        max_days = 30
        for _ in range(max_days):
            if self.is_trading_day(next_day):
                return datetime.combine(next_day.date(), self.MORNING_START)
            next_day += timedelta(days=1)
        
        logger.warning("无法找到下一个交易日")
        return None
    
    def get_trading_status(self, dt: Optional[datetime] = None) -> dict:
        if dt is None:
            dt = datetime.now()
        
        is_trading = self.is_trading_time(dt)
        session = self.get_current_session(dt)
        session_name = self.get_session_name(dt)
        next_trading = self.get_next_trading_time(dt)
        
        status = {
            'is_trading_time': is_trading,
            'is_trading_day': self.is_trading_day(dt),
            'current_session': session_name,
            'current_time': dt.strftime('%Y-%m-%d %H:%M:%S'),
            'next_trading_time': next_trading.strftime('%Y-%m-%d %H:%M:%S') if next_trading else None
        }
        
        if is_trading:
            session_end = session[1]
            end_dt = datetime.combine(dt.date(), session_end)
            remaining = end_dt - dt
            status['session_end_time'] = session_end.strftime('%H:%M:%S')
            status['remaining_seconds'] = int(remaining.total_seconds())
        
        return status


_trading_hours_instance = TradingHours()


def is_trading_time(dt: Optional[datetime] = None) -> bool:
    return _trading_hours_instance.is_trading_time(dt)


def get_next_trading_time(dt: Optional[datetime] = None) -> Optional[datetime]:
    return _trading_hours_instance.get_next_trading_time(dt)


def get_trading_status(dt: Optional[datetime] = None) -> dict:
    return _trading_hours_instance.get_trading_status(dt)
