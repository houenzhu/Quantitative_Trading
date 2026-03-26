from datetime import datetime, time, timedelta
from typing import Optional, Tuple, Set
import logging

try:
    import holidays
    HOLIDAYS_AVAILABLE = True
except ImportError:
    HOLIDAYS_AVAILABLE = False
    logging.warning("holidays库未安装，将使用内置节假日列表。请运行: pip install holidays")

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
    
    MAKEUP_TRADING_DAYS = {
        "2024-02-04",
        "2024-02-18",
        "2024-04-07",
        "2024-04-28",
        "2024-05-11",
        "2024-09-14",
        "2024-09-29",
        "2024-10-12",
        "2025-01-26",
        "2025-02-08",
        "2025-04-27",
        "2026-01-25",
        "2026-04-26",
        "2026-05-09",
        "2026-09-27",
        "2026-10-10",
    }
    
    ADDITIONAL_HOLIDAYS = {
        "2024-02-09",
        "2025-01-27",
        "2026-02-07",
    }
    
    def __init__(self, custom_holidays: Optional[Set[str]] = None, custom_makeup_days: Optional[Set[str]] = None):
        self._cn_holidays = None
        self._holiday_years = set()
        
        if custom_holidays:
            self.ADDITIONAL_HOLIDAYS.update(custom_holidays)
        if custom_makeup_days:
            self.MAKEUP_TRADING_DAYS.update(custom_makeup_days)
    
    def _get_cn_holidays(self, year: int):
        if year not in self._holiday_years:
            self._holiday_years.add(year)
            
            if HOLIDAYS_AVAILABLE:
                if not hasattr(self, '_cn_holidays') or self._cn_holidays is None:
                    self._cn_holidays = holidays.CN(years=year)
                else:
                    self._cn_holidays.expand(years=year)
        
        return self._cn_holidays
    
    def is_holiday(self, dt: datetime) -> bool:
        date_str = dt.strftime("%Y-%m-%d")
        
        if date_str in self.MAKEUP_TRADING_DAYS:
            return False
        
        if date_str in self.ADDITIONAL_HOLIDAYS:
            return True
        
        if HOLIDAYS_AVAILABLE:
            cn_holidays = self._get_cn_holidays(dt.year)
            return dt.date() in cn_holidays
        
        return False
    
    def is_weekend(self, dt: datetime) -> bool:
        date_str = dt.strftime("%Y-%m-%d")
        
        if date_str in self.MAKEUP_TRADING_DAYS:
            return False
        
        return dt.weekday() >= 5
    
    def is_trading_day(self, dt: datetime) -> bool:
        if self.is_holiday(dt):
            return False
        
        if self.is_weekend(dt):
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
    
    def get_holiday_name(self, dt: datetime) -> Optional[str]:
        if HOLIDAYS_AVAILABLE:
            cn_holidays = self._get_cn_holidays(dt.year)
            return cn_holidays.get(dt.date())
        return None


_trading_hours_instance = TradingHours()


def is_trading_time(dt: Optional[datetime] = None) -> bool:
    return _trading_hours_instance.is_trading_time(dt)


def get_next_trading_time(dt: Optional[datetime] = None) -> Optional[datetime]:
    return _trading_hours_instance.get_next_trading_time(dt)


def get_trading_status(dt: Optional[datetime] = None) -> dict:
    return _trading_hours_instance.get_trading_status(dt)


def is_trading_day(dt: Optional[datetime] = None) -> bool:
    if dt is None:
        dt = datetime.now()
    return _trading_hours_instance.is_trading_day(dt)


def get_holiday_name(dt: Optional[datetime] = None) -> Optional[str]:
    if dt is None:
        dt = datetime.now()
    return _trading_hours_instance.get_holiday_name(dt)
