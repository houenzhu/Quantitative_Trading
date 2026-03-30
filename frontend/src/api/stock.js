import request from "@/utils/request.js"

export const stockApi = {
  getStockPool() {
    return request.get('/api/stock/pool')
  },
  
  getStockPoolList() {
    return request.get('/api/stock/pool/list')
  },
  
  addStock(stockCode, stockName) {
    return request.post('/api/stock/pool/add', null, {
      params: { stockCode, stockName }
    })
  },
  
  removeStock(stockCode) {
    return request.post('/api/stock/pool/remove', null, {
      params: { stockCode }
    })
  },
  
  checkInPool(stockCode) {
    return request.get(`/api/stock/pool/check/${stockCode}`)
  }
}
