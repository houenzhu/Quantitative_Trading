import { ref, reactive } from 'vue'
import { Client } from '@stomp/stompjs'

const stockPool = ref({})
const stockData = reactive({})
const statistics = reactive({})
const positions = ref([])
const orders = ref([])
const tickHistory = reactive({})
const searchResults = ref([])
const actionMessage = ref('')
const accountInfo = ref(null)

let stompClient = null
let reconnectTimer = null
let connectionCallback = null
let subscriptions = []
let updateCallbacks = {
  onOrderUpdate: null,
  onPositionUpdate: null,
  onAccountUpdate: null,
  onSignalUpdate: null
}

const getCurrentUserId = () => {
  try {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      const user = JSON.parse(userInfo)
      return user.id || user.userId || user.user_id
    }
  } catch (e) {
    console.error('获取用户ID失败:', e)
  }
  return null
}

export function useWebSocket() {
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
    
    const userId = getCurrentUserId()
    console.log('当前用户ID:', userId)
    
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
      stompClient.subscribe('/topic/search', (message) => {
        try {
          const data = JSON.parse(message.body)
          searchResults.value = data || []
        } catch (error) {
          console.error('解析搜索结果失败:', error)
        }
      })
    )
    
    if (userId) {
      console.log('订阅用户特定topic: /topic/user/' + userId + '/orders')
      subscriptions.push(
        stompClient.subscribe('/topic/user/' + userId + '/orders', (message) => {
          try {
            const order = JSON.parse(message.body)
            console.log('收到用户订单更新:', order)
            handleOrderUpdate(order)
          } catch (error) {
            console.error('解析订单数据失败:', error)
          }
        })
      )
      
      subscriptions.push(
        stompClient.subscribe('/topic/user/' + userId + '/positions', (message) => {
          try {
            const position = JSON.parse(message.body)
            console.log('收到用户持仓更新:', position)
            handlePositionUpdate(position)
          } catch (error) {
            console.error('解析持仓数据失败:', error)
          }
        })
      )
      
      subscriptions.push(
        stompClient.subscribe('/topic/user/' + userId + '/account', (message) => {
          try {
            const account = JSON.parse(message.body)
            console.log('收到用户账户更新:', account)
            handleAccountUpdate(account)
          } catch (error) {
            console.error('解析账户数据失败:', error)
          }
        })
      )
      
      subscriptions.push(
        stompClient.subscribe('/topic/user/' + userId + '/signals', (message) => {
          try {
            const signal = JSON.parse(message.body)
            console.log('收到用户信号更新:', signal)
            if (updateCallbacks.onSignalUpdate) {
              updateCallbacks.onSignalUpdate(signal)
            }
          } catch (error) {
            console.error('解析信号数据失败:', error)
          }
        })
      )
      
      subscriptions.push(
        stompClient.subscribe('/topic/user/' + userId + '/stockPool', (message) => {
          try {
            const data = JSON.parse(message.body)
            console.log('收到用户股票池更新:', data)
            if (data.stockPool) {
              stockPool.value = data.stockPool
            }
            if (data.action && data.stockCode) {
              const actionText = data.action === 'add' ? '添加' : '移除'
              actionMessage.value = `${actionText}股票 ${data.stockCode} ${data.stockName || ''}`
            }
          } catch (error) {
            console.error('解析股票池数据失败:', error)
          }
        })
      )
    } else {
      subscriptions.push(
        stompClient.subscribe('/topic/orders', (message) => {
          try {
            const order = JSON.parse(message.body)
            console.log('收到订单更新:', order)
            handleOrderUpdate(order)
          } catch (error) {
            console.error('解析订单数据失败:', error)
          }
        })
      )
      
      subscriptions.push(
        stompClient.subscribe('/topic/positions', (message) => {
          try {
            const position = JSON.parse(message.body)
            console.log('收到持仓更新:', position)
            handlePositionUpdate(position)
          } catch (error) {
            console.error('解析持仓数据失败:', error)
          }
        })
      )
      
      subscriptions.push(
        stompClient.subscribe('/topic/account', (message) => {
          try {
            const account = JSON.parse(message.body)
            console.log('收到账户更新:', account)
            handleAccountUpdate(account)
          } catch (error) {
            console.error('解析账户数据失败:', error)
          }
        })
      )
      
      subscriptions.push(
        stompClient.subscribe('/topic/signals', (message) => {
          try {
            const signal = JSON.parse(message.body)
            console.log('收到信号更新:', signal)
            if (updateCallbacks.onSignalUpdate) {
              updateCallbacks.onSignalUpdate(signal)
            }
          } catch (error) {
            console.error('解析信号数据失败:', error)
          }
        })
      )
      
      subscriptions.push(
        stompClient.subscribe('/topic/stockPool', (message) => {
          try {
            const data = JSON.parse(message.body)
            console.log('收到股票池更新:', data)
            if (data.stockPool) {
              stockPool.value = data.stockPool
            }
            if (data.action && data.stockCode) {
              const actionText = data.action === 'add' ? '添加' : '移除'
              actionMessage.value = `${actionText}股票 ${data.stockCode} ${data.stockName || ''}`
            }
          } catch (error) {
            console.error('解析股票池数据失败:', error)
          }
        })
      )
    }
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
    
    updatePositionPrice(stockCode, tick.price)
  }

  const handleOrderUpdate = (order) => {
    if (!order) return
    
    const existingIndex = orders.value.findIndex(o => o && o.order_id === order.orderId)
    const formattedOrder = {
      order_id: order.orderId,
      stock_code: order.stockCode,
      stock_name: order.stockName,
      side: order.side,
      quantity: order.quantity,
      price: order.price,
      executed_price: order.avgFillPrice,
      status: getStatusText(order.status),
      created_at: formatDate(order.createdAt),
      reason: order.reason
    }
    
    if (existingIndex >= 0) {
      orders.value[existingIndex] = formattedOrder
    } else {
      orders.value.unshift(formattedOrder)
      if (orders.value.length > 100) {
        orders.value.pop()
      }
    }
    
    if (updateCallbacks.onOrderUpdate) {
      updateCallbacks.onOrderUpdate(formattedOrder)
    }
  }

  const handlePositionUpdate = (position) => {
    if (!position) return
    
    const existingIndex = positions.value.findIndex(p => p && p.stock_code === position.stockCode)
    const formattedPosition = {
      stock_code: position.stockCode,
      stock_name: position.stockName,
      quantity: position.quantity || 0,
      avg_price: position.avgCost,
      current_price: position.currentPrice,
      market_value: position.marketValue,
      profit_loss: position.unrealizedPnl,
      profit_loss_pct: position.unrealizedPnlPct
    }
    
    if (existingIndex >= 0) {
      if (position.quantity <= 0 || !position.isActive) {
        positions.value.splice(existingIndex, 1)
      } else {
        positions.value[existingIndex] = formattedPosition
      }
    } else if (position.quantity > 0 && position.isActive) {
      positions.value.push(formattedPosition)
    }
    
    if (updateCallbacks.onPositionUpdate) {
      updateCallbacks.onPositionUpdate(formattedPosition)
    }
  }

  const handleAccountUpdate = (account) => {
    if (!account) return
    
    accountInfo.value = account
    
    const activePositions = positions.value.filter(p => p && p.quantity > 0)
    
    statistics['总资产'] = formatMoney(account.totalEquity)
    statistics['总收益率'] = formatPercent(account.totalPnl, account.initialCapital)
    statistics['持仓数量'] = activePositions.length
    statistics['交易次数'] = account.tradeCount || 0
    statistics['胜率'] = formatWinRate(account.winCount, account.lossCount)
    
    console.log('账户更新:', {
      totalEquity: account.totalEquity,
      totalPnl: account.totalPnl,
      tradeCount: account.tradeCount,
      winCount: account.winCount,
      lossCount: account.lossCount
    })
    
    if (updateCallbacks.onAccountUpdate) {
      updateCallbacks.onAccountUpdate(account)
    }
  }

  const updatePositionPrice = (stockCode, currentPrice) => {
    if (!currentPrice) return
    const position = positions.value.find(p => p && p.stock_code === stockCode)
    if (position) {
      position.current_price = currentPrice
      position.market_value = currentPrice * position.quantity
      if (position.avg_price && position.avg_price > 0) {
        position.profit_loss = (currentPrice - position.avg_price) * position.quantity
        position.profit_loss_pct = ((currentPrice - position.avg_price) / position.avg_price * 100).toFixed(2)
      }
    }
  }

  const getStatusText = (status) => {
    const statusMap = {
      'pending': '待执行',
      'submitted': '已提交',
      'partial': '部分成交',
      'filled': '已成交',
      'cancelled': '已撤销',
      'rejected': '已拒绝'
    }
    return statusMap[status] || status
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  }

  const formatMoney = (value) => {
    if (!value) return '¥0.00'
    return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
  }

  const formatPercent = (pnl, capital) => {
    if (!pnl || !capital || capital === 0) return '0.00%'
    return (pnl / capital * 100).toFixed(2) + '%'
  }

  const formatWinRate = (winCount, lossCount) => {
    const total = (winCount || 0) + (lossCount || 0)
    if (total === 0) return '0.00%'
    return ((winCount || 0) / total * 100).toFixed(2) + '%'
  }

  const sendMessage = (destination, body) => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: destination,
        body: JSON.stringify(body)
      })
    }
  }

  const searchStock = (keyword) => {
    console.log('发送搜索请求:', keyword)
    sendMessage('/app/search', { keyword: keyword })
  }

  const setUpdateCallbacks = (callbacks) => {
    updateCallbacks = { ...updateCallbacks, ...callbacks }
  }

  const resubscribe = () => {
    console.log('重新订阅topic')
    subscribeToTopics()
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
    accountInfo,
    connect,
    disconnect,
    sendMessage,
    searchStock,
    setUpdateCallbacks,
    handleOrderUpdate,
    handlePositionUpdate,
    handleAccountUpdate,
    resubscribe
  }
}
