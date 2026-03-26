import { ref, reactive } from 'vue'
import { Client } from '@stomp/stompjs'

export function useWebSocket() {
  const stockPool = ref({})
  const stockData = reactive({})
  const statistics = reactive({})
  const positions = ref([])
  const orders = ref([])
  const tickHistory = reactive({})
  const searchResults = ref([])
  const actionMessage = ref('')
  
  let stompClient = null
  let reconnectTimer = null
  let connectionCallback = null
  let subscriptions = []

  const connect = (onConnectionChange) => {
    connectionCallback = onConnectionChange
    
    if (stompClient && stompClient.connected) {
      return
    }
    
    stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      
      onConnect: (frame) => {
        console.log('STOMP连接成功:', frame)
        if (connectionCallback) {
          connectionCallback('connected')
        }
        if (reconnectTimer) {
          clearTimeout(reconnectTimer)
          reconnectTimer = null
        }
        subscribeToTopics()
        requestInitialData()
      },
      
      onDisconnect: () => {
        console.log('STOMP连接断开')
        if (connectionCallback) {
          connectionCallback('disconnected')
        }
      },
      
      onStompError: (frame) => {
        console.error('STOMP错误:', frame.headers['message'])
        if (connectionCallback) {
          connectionCallback('error')
        }
      },
      
      onWebSocketError: (event) => {
        console.error('WebSocket错误:', event)
        if (connectionCallback) {
          connectionCallback('error')
        }
      }
    })
    
    stompClient.activate()
  }

  const subscribeToTopics = () => {
    if (!stompClient || !stompClient.connected) return
    
    subscriptions.forEach(sub => {
      try {
        sub.unsubscribe()
      } catch (e) {}
    })
    subscriptions = []
    
    subscriptions.push(
      stompClient.subscribe('/topic/init', (message) => {
        try {
          const data = JSON.parse(message.body)
          handleInitData(data)
        } catch (error) {
          console.error('解析初始化数据失败:', error)
        }
      })
    )
    
    subscriptions.push(
      stompClient.subscribe('/topic/market', (message) => {
        try {
          const data = JSON.parse(message.body)
          handleMarketData(data)
        } catch (error) {
          console.error('解析行情数据失败:', error)
        }
      })
    )
    
    subscriptions.push(
      stompClient.subscribe('/topic/stockPool', (message) => {
        try {
          const data = JSON.parse(message.body)
          handleStockPoolUpdate(data)
        } catch (error) {
          console.error('解析股票池数据失败:', error)
        }
      })
    )
    
    subscriptions.push(
      stompClient.subscribe('/topic/search', (message) => {
        try {
          const data = JSON.parse(message.body)
          searchResults.value = data || []
        } catch (error) {
          console.error('解析搜索结果失败:', error)
        }
      })
    )
    
    subscriptions.push(
      stompClient.subscribe('/topic/orders', (message) => {
        try {
          const data = JSON.parse(message.body)
          handleOrdersUpdate(data)
        } catch (error) {
          console.error('解析订单数据失败:', error)
        }
      })
    )
    
    subscriptions.push(
      stompClient.subscribe('/topic/positions', (message) => {
        try {
          const data = JSON.parse(message.body)
          handlePositionsUpdate(data)
        } catch (error) {
          console.error('解析持仓数据失败:', error)
        }
      })
    )
    
    subscriptions.push(
      stompClient.subscribe('/topic/account', (message) => {
        try {
          const data = JSON.parse(message.body)
          handleAccountUpdate(data)
        } catch (error) {
          console.error('解析账户数据失败:', error)
        }
      })
    )
    
    subscriptions.push(
      stompClient.subscribe('/topic/signals', (message) => {
        try {
          const data = JSON.parse(message.body)
          handleSignalUpdate(data)
        } catch (error) {
          console.error('解析信号数据失败:', error)
        }
      })
    )
  }

  const requestInitialData = () => {
    sendMessage('/app/init', {})
  }

  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    subscriptions.forEach(sub => {
      try {
        sub.unsubscribe()
      } catch (e) {}
    })
    subscriptions = []
    if (stompClient) {
      stompClient.deactivate()
      stompClient = null
    }
  }

  const handleInitData = (data) => {
    console.log('收到初始化数据:', data)
    
    if (data.stockPool) {
      stockPool.value = { ...data.stockPool }
      Object.keys(data.stockPool).forEach(code => {
        if (!stockData[code]) {
          stockData[code] = {
            stock_code: code,
            stock_name: data.stockPool[code],
            price: 0,
            change_percent: 0
          }
        }
        if (!tickHistory[code]) {
          tickHistory[code] = []
        }
      })
    }
    
    if (data.positions) {
      positions.value = data.positions
    }
    
    if (data.orders) {
      orders.value = data.orders
    }
    
    if (data.account) {
      Object.assign(statistics, {
        totalEquity: data.account.totalEquity,
        cashBalance: data.account.cashBalance,
        marketValue: data.account.marketValue,
        totalPnl: data.account.totalPnl,
        totalPnlPercent: data.account.totalPnlPercent,
        unrealizedPnl: data.account.unrealizedPnl,
        realizedPnl: data.account.realizedPnl,
        dailyPnl: data.account.dailyPnl,
        tradeCount: data.account.tradeCount,
        winCount: data.account.winCount,
        lossCount: data.account.lossCount
      })
    }
  }

  const handleMarketData = (data) => {
    if (Array.isArray(data)) {
      data.forEach(tick => processTickData(tick))
    } else if (typeof data === 'object') {
      processTickData(data)
    }
  }

  const processTickData = (tick) => {
    if (!tick || !tick.code) return
    
    const stockCode = tick.code
    const changePercent = tick.changePct || tick.changePercent || tick.change_percent || 0
    const timestamp = tick.time || tick.timestamp || new Date().toISOString()
    
    stockData[stockCode] = {
      ...stockData[stockCode],
      stock_code: stockCode,
      stock_name: tick.name || stockData[stockCode]?.stock_name,
      price: tick.price,
      change_percent: changePercent,
      open: tick.open,
      high: tick.high,
      low: tick.low,
      volume: tick.volume,
      amount: tick.amount,
      timestamp: timestamp
    }
    
    if (!tickHistory[stockCode]) {
      tickHistory[stockCode] = []
    }
    tickHistory[stockCode].push({
      ...tick,
      stock_code: stockCode,
      change_percent: changePercent,
      timestamp: timestamp
    })
    if (tickHistory[stockCode].length > 100) {
      tickHistory[stockCode].shift()
    }
    
    console.log('处理行情数据:', stockCode, tick.price, changePercent)
  }

  const handleStockPoolUpdate = (data) => {
    if (data.stockPool) {
      const oldCodes = Object.keys(stockPool.value)
      const newCodes = Object.keys(data.stockPool)
      
      const removed = oldCodes.filter(code => !newCodes.includes(code))
      removed.forEach(code => {
        delete stockData[code]
        delete tickHistory[code]
      })
      
      stockPool.value = { ...data.stockPool }
      
      newCodes.forEach(code => {
        if (!stockData[code]) {
          stockData[code] = {
            stock_code: code,
            stock_name: data.stockPool[code],
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
  }

  const handleOrdersUpdate = (data) => {
    if (Array.isArray(data)) {
      orders.value = data
    } else if (data) {
      orders.value = [data]
    }
  }

  const handlePositionsUpdate = (data) => {
    if (Array.isArray(data)) {
      positions.value = data
    } else if (data) {
      positions.value = [data]
    }
  }

  const handleAccountUpdate = (data) => {
    if (data) {
      Object.assign(statistics, {
        totalEquity: data.totalEquity,
        cashBalance: data.cashBalance,
        marketValue: data.marketValue,
        totalPnl: data.totalPnl,
        totalPnlPercent: data.totalPnlPercent
      })
    }
  }

  const handleSignalUpdate = (data) => {
    console.log('收到交易信号:', data)
  }

  const sendMessage = (destination, body) => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: destination,
        body: JSON.stringify(body)
      })
    }
  }

  const subscribeStock = (stockCode) => {
    if (!stompClient || !stompClient.connected) return
    
    const subscription = stompClient.subscribe(`/topic/tick/${stockCode}`, (message) => {
      try {
        const data = JSON.parse(message.body)
        processTickData(data)
      } catch (error) {
        console.error('解析股票行情失败:', error)
      }
    })
    subscriptions.push(subscription)
    
    sendMessage('/app/subscribe/' + stockCode, {})
  }

  const getHistory = (stockCode) => {
    sendMessage('/app/history/' + stockCode, {})
  }

  const getAllHistory = () => {
    sendMessage('/app/history/all', {})
  }

  const searchStock = (keyword) => {
    console.log('发送搜索请求:', keyword)
    sendMessage('/app/search', { keyword: keyword })
  }

  const addStock = (code, name) => {
    sendMessage('/app/stock/add', { code: code, name: name })
  }

  const removeStock = (code) => {
    sendMessage('/app/stock/remove', { code: code })
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
    subscribeStock,
    getHistory,
    getAllHistory,
    searchStock,
    addStock,
    removeStock
  }
}
