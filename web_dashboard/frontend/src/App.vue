<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="header-left">
        <el-icon class="logo-icon"><TrendCharts /></el-icon>
        <span class="app-title">量化交易监控系统</span>
      </div>
      <div class="header-right">
        <el-tag :type="connectionStatus === 'connected' ? 'success' : 'danger'" effect="plain">
          <el-icon><Connection /></el-icon>
          {{ connectionStatus === 'connected' ? '已连接' : '未连接' }}
        </el-tag>
        <el-text type="info">{{ currentTime }}</el-text>
      </div>
    </el-header>
    <el-main class="app-main">
      <Dashboard />
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted, provide } from 'vue'
import Dashboard from './components/Dashboard.vue'
import { useWebSocket } from './utils/websocket'

const currentTime = ref('')
const connectionStatus = ref('disconnected')

const {
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
  searchStock,
  addStock,
  removeStock
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
provide('addStock', addStock)
provide('removeStock', removeStock)

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

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
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

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-main {
  padding: 20px;
  overflow-y: auto;
  background-color: #f5f7fa;
}
</style>
