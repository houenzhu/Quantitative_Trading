import request from "@/utils/request.js"

export const orderApi = {
  getRecentOrders(limit = 100) {
    return request.get('/api/order/recent', { params: { limit } })
  },
  
  getOrderByOrderId(orderId) {
    return request.get(`/api/order/${orderId}`)
  },
  
  getOrdersByStockCode(stockCode) {
    return request.get(`/api/order/stock/${stockCode}`)
  },
  
  createOrder(data) {
    return request.post('/api/order/create', null, { params: data })
  },
  
  updateOrderStatus(orderId, status) {
    return request.post('/api/order/status', null, {
      params: { orderId, status }
    })
  }
}
