import request from "@/utils/request.js"

export const authApi = {
  login(data) {
    return request.post('/api/auth/login', data)
  },
  
  register(data) {
    return request.post('/api/auth/register', data)
  },
  
  logout() {
    return request.post('/api/auth/logout')
  },
  
  getUserInfo() {
    return request.get('/api/auth/info')
  },
  
  checkLogin() {
    return request.get('/api/auth/check')
  }
}
