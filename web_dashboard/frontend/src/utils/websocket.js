import { ref, reactive } from 'vue'

export function useWebSocket() {
  const stockPool = ref({})
  const stockData = reactive({})
  const statistics = reactive({})
  const positions = ref([])
  const orders = ref([])
  const tickHistory = reactive({})
  const searchResults = ref([])
  const actionMessage = ref('')
  
  let ws = null
  let reconnectTimer = null
  let connectionCallback = null

  const connect = (onConnectionChange) => {
    connectionCallback = onConnectionChange
    
    const wsUrl = 'ws://localhost:8765'
    
    ws = new WebSocket(wsUrl)
    
    ws.onopen = () => {
      console.log('WebSocket连接成功')
      if (connectionCallback) {
        connectionCallback('connected')
      }
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
    }
    
    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        handleMessage(message)
      } catch (error) {
        console.error('解析消息失败:', error)
      }
    }
    
    ws.onclose = () => {
      console.log('WebSocket连接关闭')
      if (connectionCallback) {
        connectionCallback('disconnected')
      }
      reconnect()
    }
    
    ws.onerror = (error) => {
      console.error('WebSocket错误:', error)
      if (connectionCallback) {
        connectionCallback('error')
      }
    }
  }

  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.close()
      ws = null
    }
  }

  const reconnect = () => {
    if (reconnectTimer) return
    
    reconnectTimer = setTimeout(() => {
      console.log('尝试重新连接...')
      connect(connectionCallback)
    }, 5000)
  }

  const handleMessage = (message) => {
    const { type, data } = message
    
    switch (type) {
      case 'init':
        if (data.stock_pool) {
          stockPool.value = { ...data.stock_pool }
          Object.keys(data.stock_pool).forEach(code => {
            stockData[code] = {
              stock_code: code,
              stock_name: data.stock_pool[code],
              price: 0,
              change_percent: 0
            }
            if (!tickHistory[code]) {
              tickHistory[code] = []
            }
          })
        }
        if (data.statistics) {
          Object.assign(statistics, data.statistics)
        }
        if (data.positions) {
          positions.value = data.positions
        }
        if (data.orders) {
          orders.value = data.orders
        }
        break
        
      case 'stock_pool_update':
        if (data.stock_pool) {
          const oldCodes = Object.keys(stockPool.value)
          const newCodes = Object.keys(data.stock_pool)
          
          const removed = oldCodes.filter(code => !newCodes.includes(code))
          removed.forEach(code => {
            delete stockData[code]
            delete tickHistory[code]
          })
          
          stockPool.value = { ...data.stock_pool }
          
          newCodes.forEach(code => {
            if (!stockData[code]) {
              stockData[code] = {
                stock_code: code,
                stock_name: data.stock_pool[code],
                price: 0,
                change_percent: 0
              }
            }
            if (!tickHistory[code]) {
              tickHistory[code] = []
            }
          })
          
          console.log('股票池已更新:', newCodes)
        }
        break
        
      case 'tick':
        if (data.stock_code) {
          stockData[data.stock_code] = {
            ...stockData[data.stock_code],
            ...data
          }
          
          if (!tickHistory[data.stock_code]) {
            tickHistory[data.stock_code] = []
          }
          tickHistory[data.stock_code].push(data)
          if (tickHistory[data.stock_code].length > 100) {
            tickHistory[data.stock_code].shift()
          }
        }
        break
        
      case 'statistics':
        if (data) {
          Object.assign(statistics, data)
        }
        break
        
      case 'positions':
        if (data) {
          positions.value = data
        }
        break
        
      case 'orders':
        if (data) {
          orders.value = data
        }
        break
        
      case 'history':
        if (data && message.stock_code) {
          tickHistory[message.stock_code] = data
        }
        break
        
      case 'all_history':
        if (data) {
          Object.assign(tickHistory, data)
        }
        break
        
      case 'search_result':
        console.log('收到搜索结果:', data)
        searchResults.value = data || []
        break
        
      case 'stock_action_result':
        actionMessage.value = data.message
        setTimeout(() => {
          actionMessage.value = ''
        }, 3000)
        break
    }
  }

  const sendMessage = (message) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(message))
    }
  }

  const getHistory = (stockCode) => {
    sendMessage({
      type: 'get_history',
      stock_code: stockCode
    })
  }

  const getAllHistory = () => {
    sendMessage({
      type: 'get_all_history'
    })
  }

  const searchStock = (keyword) => {
    console.log('发送搜索请求:', keyword)
    sendMessage({
      type: 'search_stock',
      keyword: keyword
    })
  }

  const addStock = (code, name) => {
    sendMessage({
      type: 'add_stock',
      code: code,
      name: name
    })
  }

  const removeStock = (code) => {
    sendMessage({
      type: 'remove_stock',
      code: code
    })
  }

  return {
    stockPool,
    stockData,
    statistics,
    positions,
    orders,
    tickHistory,
    searchResults,
    actionMessage,
    connect,
    disconnect,
    sendMessage,
    getHistory,
    getAllHistory,
    searchStock,
    addStock,
    removeStock
  }
}
