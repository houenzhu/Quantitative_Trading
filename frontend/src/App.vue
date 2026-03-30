<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="header-left">
        <el-icon class="logo-icon"><TrendCharts /></el-icon>
        <span class="app-title">量化交易监控系统</span>
      </div>
      
      <el-menu 
        :default-active="activeMenu" 
        mode="horizontal" 
        @select="handleMenuSelect"
        class="nav-menu"
      >
        <el-menu-item index="dashboard">
          <el-icon><DataLine /></el-icon>
          <span>交易监控</span>
        </el-menu-item>
        <el-menu-item index="strategy">
          <el-icon><Operation /></el-icon>
          <span>策略管理</span>
        </el-menu-item>
      </el-menu>
      
      <div class="header-right">
        <el-tag :type="connectionStatus === 'connected' ? 'success' : 'danger'" effect="plain">
          <el-icon><Connection /></el-icon>
          {{ connectionStatus === 'connected' ? '已连接' : '未连接' }}
        </el-tag>
        <el-text type="info">{{ currentTime }}</el-text>
        
        <div class="user-section">
          <template v-if="isLoggedIn">
            <el-dropdown @command="handleCommand">
              <span class="user-dropdown">
                <el-avatar :size="32" :src="userInfo?.avatar">
                  {{ userInfo?.nickname?.charAt(0) || userInfo?.username?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="username">{{ userInfo?.nickname || userInfo?.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人信息
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon><Setting /></el-icon>
                    账户设置
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="showAuthDialog('login')">登录</el-button>
            <el-button @click="showAuthDialog('register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="app-main">
      <Dashboard v-if="activeMenu === 'dashboard'" />
      <StrategyManager v-if="activeMenu === 'strategy'" />
    </el-main>
    
    <AuthDialog
      v-model="authDialogVisible"
      :mode="authDialogMode"
      @login-success="handleLoginSuccess"
    />
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted, provide, computed } from 'vue'
import Dashboard from './components/Dashboard.vue'
import AuthDialog from './components/AuthDialog.vue'
import StrategyManager from './components/StrategyManager.vue'
import { useWebSocket } from './utils/websocket'
import { authApi } from './api/auth'
import { stockApi } from './api/stock'
import { orderApi } from './api/order'
import { positionApi } from './api/position'
import { accountApi } from './api/account'
import { ElMessage, ElMessageBox } from 'element-plus'

const currentTime = ref('')
const connectionStatus = ref('disconnected')
const authDialogVisible = ref(false)
const authDialogMode = ref('login')
const userInfo = ref(null)
const activeMenu = ref('dashboard')

const isLoggedIn = computed(() => !!userInfo.value)

const handleMenuSelect = (index) => {
  activeMenu.value = index
}

const {
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
  searchStock,
  setUpdateCallbacks,
  handleOrderUpdate,
  handlePositionUpdate,
  handleAccountUpdate,
  resubscribe
} = useWebSocket()

provide('stockPool', stockPool)
provide('stockData', stockData)
provide('statistics', statistics)
provide('positions', positions)
provide('orders', orders)
provide('tickHistory', tickHistory)
provide('searchResults', searchResults)
provide('actionMessage', actionMessage)
provide('searchStock', searchStock)
provide('userInfo', userInfo)
provide('accountInfo', accountInfo)
provide('setUpdateCallbacks', setUpdateCallbacks)
provide('handleOrderUpdate', handleOrderUpdate)
provide('handlePositionUpdate', handlePositionUpdate)
provide('handleAccountUpdate', handleAccountUpdate)

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const handleConnectionChange = (status) => {
  connectionStatus.value = status
}

const showAuthDialog = (mode) => {
  authDialogMode.value = mode
  authDialogVisible.value = true
}

const loadUserData = async () => {
  try {
    const [stockPoolRes, positionsRes, ordersRes, accountRes] = await Promise.all([
      stockApi.getStockPool(),
      positionApi.getActivePositions(),
      orderApi.getRecentOrders(100),
      accountApi.getLatestAccount()
    ])
    
    if (stockPoolRes.code === 200) {
      stockPool.value = stockPoolRes.data || {}
    }
    
    if (positionsRes.code === 200) {
      positions.value = (positionsRes.data || []).map(p => ({
        stock_code: p.stockCode,
        stock_name: p.stockName,
        quantity: p.quantity,
        avg_price: p.avgCost,
        current_price: p.currentPrice,
        market_value: p.marketValue,
        profit_loss: p.unrealizedPnl,
        profit_loss_pct: p.unrealizedPnlPct
      }))
    }
    
    if (ordersRes.code === 200) {
      orders.value = (ordersRes.data || []).map(o => ({
        order_id: o.orderId,
        stock_code: o.stockCode,
        stock_name: o.stockName,
        side: o.side,
        quantity: o.quantity,
        price: o.price,
        executed_price: o.avgFillPrice,
        status: getStatusText(o.status),
        created_at: formatDate(o.createdAt),
        reason: o.reason
      }))
    }
    
    if (accountRes.code === 200 && accountRes.data) {
      const acc = accountRes.data
      Object.assign(statistics, {
        '总资产': formatMoney(acc.totalEquity),
        '总收益率': formatPercent(acc.totalPnl, acc.initialCapital),
        '持仓数量': positions.value.length,
        '交易次数': acc.tradeCount || 0,
        '胜率': formatWinRate(acc.winCount, acc.lossCount)
      })
    }
  } catch (e) {
    console.error('加载用户数据失败', e)
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

const handleLoginSuccess = async (user) => {
  userInfo.value = user
  resubscribe()
  await loadUserData()
}

const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人信息功能开发中')
      break
    case 'settings':
      ElMessage.info('账户设置功能开发中')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await authApi.logout()
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        userInfo.value = null
        stockPool.value = {}
        positions.value = []
        orders.value = []
        Object.keys(statistics).forEach(key => delete statistics[key])
        ElMessage.success('已退出登录')
      } catch (e) {
        if (e !== 'cancel') {
          console.error('退出登录失败', e)
        }
      }
      break
  }
}

const checkLoginStatus = async () => {
  const token = localStorage.getItem('token')
  const savedUserInfo = localStorage.getItem('userInfo')
  
  if (token && savedUserInfo) {
    try {
      const res = await authApi.checkLogin()
      if (res.code === 200 && res.data?.isLogin) {
        userInfo.value = res.data.userInfo
        await loadUserData()
      } else {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
      }
    } catch (e) {
      console.error('检查登录状态失败', e)
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
}

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
  checkLoginStatus()
  connect(handleConnectionChange)
})

onUnmounted(() => {
  disconnect()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.app-container {
  height: 100%;
  background-color: #f5f7fa;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 24px;
  color: #409eff;
}

.app-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.nav-menu {
  border-bottom: none;
  flex: 1;
  margin: 0 40px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-dropdown:hover {
  background-color: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #303133;
}

.app-main {
  padding: 20px;
  overflow-y: auto;
  background-color: #f5f7fa;
}
</style>
