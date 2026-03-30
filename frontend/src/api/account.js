import request from "@/utils/request.js"

export const accountApi = {
  getLatestAccount() {
    return request.get('/api/account/latest')
  },
  
  getRecentAccounts(limit = 10) {
    return request.get('/api/account/recent', { params: { limit } })
  },
  
  initAccount(initialCapital = 1000000) {
    return request.post('/api/account/init', null, {
      params: { initialCapital }
    })
  }
}
