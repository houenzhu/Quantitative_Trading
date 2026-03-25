import asyncio
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from websocket_server import TradingWebSocketServer


def main():
    config = {
        'initial_capital': 1000000,
        'max_daily_loss': 0.05,
        'max_position_pct': 0.2,
        'vwap_deviation': 0.005,
        'order_imbalance': 0.6,
        'momentum_threshold': 2.0,
        'strategy_weights': [0.4, 0.4, 0.2],
        'stock_pool': {
            '600547': '山东黄金',
            '600489': '中金黄金',
            '601899': '紫金矿业',
            '600988': '赤峰黄金',
            '000975': '银泰黄金',
            '002155': '湖南黄金'
        }
    }
    
    server = TradingWebSocketServer(config)
    asyncio.run(server.start(host='localhost', port=8765))


if __name__ == '__main__':
    main()
