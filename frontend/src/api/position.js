import request from "@/utils/request.js"

export const positionApi = {
  getActivePositions() {
    return request.get('/api/position/active')
  },
  
  getPositionByStockCode(stockCode) {
    return request.get(`/api/position/${stockCode}`)
  },
  
  openPosition(data) {
    return request.post('/api/position/open', null, { params: data })
  },
  
  closePosition(data) {
    return request.post('/api/position/close', null, { params: data })
  }
}
