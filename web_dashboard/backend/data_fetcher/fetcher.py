import logging
import requests
from datetime import datetime
from typing import Optional

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick

logger = logging.getLogger(__name__)


class DataFetcher:
    def __init__(self):
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'https://finance.sina.com.cn'
        }
    
    def safe_float(self, value, default=0.0):
        if value is None or value == '' or value == '--' or value == 'NaN':
            return default
        try:
            return float(str(value).strip())
        except:
            return default
    
    def safe_int(self, value, default=0):
        if value is None or value == '' or value == '--' or value == 'NaN':
            return default
        try:
            return int(float(str(value).strip()))
        except:
            return default
    
    def get_realtime_tick(self, stock_code: str, stock_name: str) -> Optional[Tick]:
        try:
            market = 'sh' if stock_code.startswith('6') else 'sz'
            url = f"https://hq.sinajs.cn/list={market}{stock_code}"
            
            response = requests.get(url, headers=self.headers, timeout=3)
            
            if response.status_code == 200:
                data = response.text.split(',')
                
                if len(data) < 33:
                    return None
                
                tick = Tick(
                    timestamp=datetime.now(),
                    stock_code=stock_code,
                    stock_name=stock_name,
                    price=self.safe_float(data[3]),
                    volume=self.safe_int(data[8]),
                    amount=self.safe_float(data[9]),
                    buy_volume=self.safe_int(data[10]),
                    sell_volume=self.safe_int(data[11]),
                    change_percent=0,
                    bid_price=self.safe_float(data[12]),
                    ask_price=self.safe_float(data[13])
                )
                
                yest_close = self.safe_float(data[2])
                if yest_close > 0:
                    tick.change_percent = (tick.price - yest_close) / yest_close * 100
                
                return tick
                
        except Exception as e:
            logger.error(f"获取{stock_code}数据失败: {e}")
        
        return None
