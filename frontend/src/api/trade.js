import request from "@/utils/request.js"

export const tradeApi = {
  getRecentTrades(limit = 100) {
    return request.get('/api/trade/recent', { params: { limit } })
  },
  
  getTradesByStockCode(stockCode) {
    return request.get(`/api/trade/stock/${stockCode}`)
  }
}
