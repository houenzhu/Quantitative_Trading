import asyncio
import json
import logging
from datetime import datetime
from typing import Dict, List, Set
import websockets
from websockets.server import WebSocketServerProtocol
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models import Tick, Order, Position
from data_fetcher import DataFetcher
from trading_system import AutoTradingSystem
from stock_searcher import StockSearcher

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

logging.getLogger('websockets').setLevel(logging.WARNING)


class TradingWebSocketServer:
    def __init__(self, config: Dict):
        self.config = config
        self.clients: Set[WebSocketServerProtocol] = set()
        
        self.trading_system = AutoTradingSystem(config)
        self.data_fetcher = DataFetcher()
        self.stock_searcher = StockSearcher()
        
        self.stock_pool = config.get('stock_pool', {
            '600547': '山东黄金',
            '600489': '中金黄金',
            '601899': '紫金矿业',
            '600988': '赤峰黄金',
            '000975': '银泰黄金',
            '002155': '湖南黄金'
        })
        
        self.tick_history: Dict[str, List[Dict]] = {code: [] for code in self.stock_pool}
        self.max_history_length = 100
        
    async def register_client(self, websocket: WebSocketServerProtocol):
        self.clients.add(websocket)
        logger.info(f"客户端连接，当前连接数: {len(self.clients)}")
        await self.send_initial_data(websocket)
    
    async def unregister_client(self, websocket: WebSocketServerProtocol):
        self.clients.discard(websocket)
        logger.info(f"客户端断开，当前连接数: {len(self.clients)}")
    
    async def send_initial_data(self, websocket: WebSocketServerProtocol):
        try:
            initial_data = {
                'type': 'init',
                'data': {
                    'stock_pool': self.stock_pool,
                    'statistics': self.trading_system.portfolio.get_statistics(),
                    'positions': self.get_positions_data(),
                    'orders': self.get_orders_data()
                }
            }
            await websocket.send(json.dumps(initial_data, ensure_ascii=False, default=self.json_serializer))
        except Exception as e:
            logger.error(f"发送初始数据失败: {e}")
    
    def json_serializer(self, obj):
        if isinstance(obj, datetime):
            return obj.strftime('%Y-%m-%d %H:%M:%S')
        if isinstance(obj, (Tick, Order, Position)):
            return obj.__dict__
        if hasattr(obj, 'value'):
            return obj.value
        raise TypeError(f"Object of type {type(obj)} is not JSON serializable")
    
    def get_positions_data(self) -> List[Dict]:
        positions = []
        for code, pos in self.trading_system.portfolio.positions.items():
            positions.append({
                'stock_code': pos.stock_code,
                'stock_name': pos.stock_name,
                'quantity': pos.quantity,
                'avg_price': round(pos.avg_price, 2),
                'current_price': round(pos.current_price, 2),
                'market_value': round(pos.market_value, 2),
                'profit_loss': round(pos.profit_loss, 2),
                'profit_loss_pct': round(pos.profit_loss_pct, 2)
            })
        return positions
    
    def get_orders_data(self) -> List[Dict]:
        orders = []
        for order in self.trading_system.executor.orders[-20:]:
            orders.append({
                'order_id': order.order_id,
                'stock_code': order.stock_code,
                'stock_name': order.stock_name,
                'side': order.side.value,
                'order_type': order.order_type.value,
                'price': round(order.price, 2),
                'quantity': order.quantity,
                'status': order.status.value,
                'created_at': order.created_at.strftime('%Y-%m-%d %H:%M:%S') if order.created_at else '',
                'executed_price': round(order.executed_price, 2) if order.executed_price else 0,
                'reason': order.reason or ''
            })
        return orders
    
    async def broadcast_data(self, message: Dict):
        if not self.clients:
            return
        
        json_message = json.dumps(message, ensure_ascii=False, default=self.json_serializer)
        tasks = [client.send(json_message) for client in self.clients]
        await asyncio.gather(*tasks, return_exceptions=True)
    
    async def add_stock(self, code: str, name: str):
        if code in self.stock_pool:
            return False, f"股票 {code} 已在股票池中"
        
        self.stock_pool[code] = name
        self.tick_history[code] = []
        self.trading_system.stock_pool = self.stock_pool
        
        if code not in self.trading_system.history_data:
            self.trading_system.history_data[code] = []
        
        logger.info(f"添加股票: {code} - {name}")
        
        await self.broadcast_data({
            'type': 'stock_pool_update',
            'data': {
                'stock_pool': self.stock_pool,
                'action': 'add',
                'code': code,
                'name': name
            }
        })
        
        return True, f"成功添加 {name}({code})"
    
    async def remove_stock(self, code: str):
        if code not in self.stock_pool:
            return False, f"股票 {code} 不在股票池中"
        
        name = self.stock_pool[code]
        del self.stock_pool[code]
        
        if code in self.tick_history:
            del self.tick_history[code]
        
        if code in self.trading_system.history_data:
            del self.trading_system.history_data[code]
        
        self.trading_system.stock_pool = self.stock_pool
        
        logger.info(f"移除股票: {code} - {name}")
        
        await self.broadcast_data({
            'type': 'stock_pool_update',
            'data': {
                'stock_pool': self.stock_pool,
                'action': 'remove',
                'code': code
            }
        })
        
        return True, f"成功移除 {name}({code})"
    
    async def fetch_and_broadcast_ticks(self):
        while True:
            try:
                for stock_code, stock_name in self.stock_pool.items():
                    tick = self.data_fetcher.get_realtime_tick(stock_code, stock_name)
                    
                    if tick:
                        self.trading_system.process_tick(tick)
                        
                        tick_data = {
                            'timestamp': tick.timestamp.strftime('%Y-%m-%d %H:%M:%S'),
                            'stock_code': tick.stock_code,
                            'stock_name': tick.stock_name,
                            'price': tick.price,
                            'volume': tick.volume,
                            'amount': tick.amount,
                            'buy_volume': tick.buy_volume,
                            'sell_volume': tick.sell_volume,
                            'change_percent': tick.change_percent,
                            'bid_price': tick.bid_price,
                            'ask_price': tick.ask_price
                        }
                        
                        self.tick_history[stock_code].append(tick_data)
                        if len(self.tick_history[stock_code]) > self.max_history_length:
                            self.tick_history[stock_code] = self.tick_history[stock_code][-self.max_history_length:]
                        
                        await self.broadcast_data({
                            'type': 'tick',
                            'data': tick_data
                        })
                
                current_prices = {
                    code: self.tick_history[code][-1]['price'] 
                    for code in self.stock_pool 
                    if self.tick_history[code]
                }
                
                if current_prices:
                    self.trading_system.portfolio.update_market_value(current_prices)
                    
                    await self.broadcast_data({
                        'type': 'statistics',
                        'data': self.trading_system.portfolio.get_statistics()
                    })
                    
                    await self.broadcast_data({
                        'type': 'positions',
                        'data': self.get_positions_data()
                    })
                    
                    await self.broadcast_data({
                        'type': 'orders',
                        'data': self.get_orders_data()
                    })
                
                await asyncio.sleep(3)
                
            except Exception as e:
                logger.error(f"获取数据失败: {e}")
                await asyncio.sleep(5)
    
    async def handle_client_message(self, websocket: WebSocketServerProtocol, message: str):
        try:
            data = json.loads(message)
            msg_type = data.get('type')
            
            if msg_type == 'get_history':
                stock_code = data.get('stock_code')
                if stock_code in self.tick_history:
                    await websocket.send(json.dumps({
                        'type': 'history',
                        'stock_code': stock_code,
                        'data': self.tick_history[stock_code]
                    }, ensure_ascii=False))
            
            elif msg_type == 'get_all_history':
                await websocket.send(json.dumps({
                    'type': 'all_history',
                    'data': self.tick_history
                }, ensure_ascii=False))
            
            elif msg_type == 'search_stock':
                keyword = data.get('keyword', '')
                results = self.stock_searcher.search_stock(keyword)
                await websocket.send(json.dumps({
                    'type': 'search_result',
                    'data': results
                }, ensure_ascii=False))
            
            elif msg_type == 'add_stock':
                code = data.get('code', '')
                name = data.get('name', '')
                success, msg = await self.add_stock(code, name)
                await websocket.send(json.dumps({
                    'type': 'stock_action_result',
                    'data': {
                        'action': 'add',
                        'success': success,
                        'message': msg
                    }
                }, ensure_ascii=False))
            
            elif msg_type == 'remove_stock':
                code = data.get('code', '')
                success, msg = await self.remove_stock(code)
                await websocket.send(json.dumps({
                    'type': 'stock_action_result',
                    'data': {
                        'action': 'remove',
                        'success': success,
                        'message': msg
                    }
                }, ensure_ascii=False))
                
        except Exception as e:
            logger.error(f"处理客户端消息失败: {e}")
    
    async def websocket_handler(self, websocket: WebSocketServerProtocol):
        await self.register_client(websocket)
        
        try:
            async for message in websocket:
                await self.handle_client_message(websocket, message)
        except websockets.exceptions.ConnectionClosed:
            pass
        finally:
            await self.unregister_client(websocket)
    
    async def start(self, host: str = 'localhost', port: int = 8765):
        logger.info(f"WebSocket服务器启动: ws://{host}:{port}")
        logger.info(f"股票池: {list(self.stock_pool.keys())}")
        
        tick_task = asyncio.create_task(self.fetch_and_broadcast_ticks())
        
        async with websockets.serve(self.websocket_handler, host, port):
            await asyncio.Future()
