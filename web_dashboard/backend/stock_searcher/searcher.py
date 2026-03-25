import re
import logging
from typing import Dict, List
import requests

logger = logging.getLogger(__name__)


class StockSearcher:
    def __init__(self):
        self.search_url = "https://suggest3.sinajs.cn/suggest/type=11,13,15,17,21,23,24,25,26,27,33,39&key="
    
    def search_stock(self, keyword: str, limit: int = 10) -> List[Dict]:
        stocks = []
        
        try:
            url = self.search_url + keyword
            response = requests.get(url, timeout=5)
            response.encoding = 'gbk'
            
            text = response.text.strip()
            logger.info(f"新浪搜索返回: {text[:200]}...")
            
            if not text or 'suggestvalue' not in text:
                return stocks
            
            match = re.search(r'var suggestvalue="(.+?)";', text)
            if not match:
                return stocks
            
            content = match.group(1)
            lines = content.split(';')
            
            for line in lines:
                if not line.strip():
                    continue
                    
                parts = line.split(',')
                if len(parts) < 5:
                    continue
                
                code_with_market = parts[0].strip()
                code = parts[2].strip() if len(parts) > 2 else ''
                name = parts[4].strip() if len(parts) > 4 else ''
                
                if code and name and code.startswith(('6', '0', '3', '68')):
                    stocks.append({
                        'code': code,
                        'name': name,
                        'market': code_with_market[:2].upper(),
                        'display': f"{code} - {name}"
                    })
                    
                    if len(stocks) >= limit:
                        break
            
        except Exception as e:
            logger.error(f"搜索股票失败: {e}")
        
        return stocks
