<template>
  <div class="dashboard">
    <el-row :gutter="20" class="statistics-section">
      <el-col :xs="24" :sm="12" :md="6">
        <StatisticsCard
          title="总资产"
          :value="statistics['总资产'] || '¥0.00'"
          icon="Wallet"
          :trend="getTrend(statistics['总收益率'])"
          highlight
        />
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <StatisticsCard
          title="总收益率"
          :value="statistics['总收益率'] || '0.00%'"
          icon="TrendCharts"
          :trend="getTrend(statistics['总收益率'])"
        />
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <StatisticsCard
          title="持仓数量"
          :value="statistics['持仓数量'] || '0'"
          icon="Briefcase"
          subtitle="只股票"
        />
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <StatisticsCard
          title="交易次数"
          :value="statistics['交易次数'] || '0'"
          icon="Document"
          :subtitle="`胜率: ${statistics['胜率'] || '0.00%'}`"
        />
      </el-col>
    </el-row>

    <el-card class="stock-manager-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><Setting /></el-icon>
          <span>股票池管理</span>
          <el-tag size="small" type="info" effect="plain">
            {{ stockPoolSize }} 只股票
          </el-tag>
        </div>
      </template>
      
      <div class="stock-manager">
        <div class="search-section">
          <el-autocomplete
            v-model="searchKeyword"
            :fetch-suggestions="handleSearch"
            placeholder="输入股票代码或名称搜索"
            @select="handleSelectStock"
            clearable
            style="width: 350px"
            value-key="display"
          >
            <template #suffix>
              <el-icon class="el-input__icon"><Search /></el-icon>
            </template>
          </el-autocomplete>
        </div>
        
        <el-alert
          v-if="actionMessage"
          :title="actionMessage"
          :type="actionMessage.includes('成功') ? 'success' : 'error'"
          show-icon
          :closable="false"
          style="margin-top: 10px;"
        />
        
        <div class="stock-tags">
          <el-tag
            v-for="(name, code) in stockPool"
            :key="code"
            closable
            @close="handleRemoveStock(code)"
            size="large"
            class="stock-tag"
          >
            {{ code }} - {{ name }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <el-card class="charts-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><DataLine /></el-icon>
          <span>实时行情</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col 
          v-for="(name, code) in stockPool" 
          :key="code"
          :xs="24" 
          :sm="12" 
          :md="8"
        >
          <StockChart 
            :stock-code="code" 
            :stock-name="name" 
          />
        </el-col>
      </el-row>
      <el-empty 
        v-if="stockPoolSize === 0" 
        description="请添加股票到股票池"
        :image-size="100"
      />
    </el-card>

    <el-row :gutter="20" class="tables-section">
      <el-col :xs="24" :lg="12">
        <PositionTable />
      </el-col>
      <el-col :xs="24" :lg="12">
        <OrderTable />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { inject, computed, ref, watch } from 'vue'
import { Setting, Plus, DataLine, Search } from '@element-plus/icons-vue'
import StatisticsCard from './StatisticsCard.vue'
import StockChart from './StockChart.vue'
import PositionTable from './PositionTable.vue'
import OrderTable from './OrderTable.vue'

const statistics = inject('statistics')
const stockPool = inject('stockPool')
const searchResults = inject('searchResults')
const actionMessage = inject('actionMessage')
const searchStock = inject('searchStock')
const addStock = inject('addStock')
const removeStock = inject('removeStock')

const searchKeyword = ref('')
const pendingCallback = ref(null)
let searchTimer = null

watch(searchResults, (newVal) => {
  console.log('搜索结果更新:', newVal)
  if (pendingCallback.value) {
    pendingCallback.value(newVal || [])
    pendingCallback.value = null
  }
}, { deep: true })

const stockPoolSize = computed(() => {
  return Object.keys(stockPool.value || {}).length
})

const getTrend = (value) => {
  if (!value) return 'neutral'
  const numStr = String(value).replace('%', '').replace('¥', '').replace(/,/g, '')
  const num = parseFloat(numStr)
  if (isNaN(num)) return 'neutral'
  if (num > 0) return 'up'
  if (num < 0) return 'down'
  return 'neutral'
}

const handleSearch = (queryString, cb) => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  
  if (!queryString || queryString.length < 1) {
    cb([])
    return
  }
  
  pendingCallback.value = cb
  
  searchTimer = setTimeout(() => {
    console.log('发送搜索请求:', queryString)
    searchStock(queryString)
  }, 300)
}

const handleSelectStock = (item) => {
  console.log('选择股票:', item)
  if (item && item.code && item.name) {
    addStock(item.code, item.name)
    searchKeyword.value = ''
  }
}

const handleRemoveStock = (code) => {
  removeStock(code)
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.statistics-section {
  margin-bottom: 0;
}

.statistics-section .el-col {
  margin-bottom: 0;
}

.stock-manager-card {
  border-radius: 8px;
}

.charts-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.card-header .el-icon {
  color: #409eff;
  font-size: 18px;
}

.stock-manager {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-section {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stock-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stock-tag {
  font-size: 14px;
}

.charts-card .el-row {
  row-gap: 16px;
}

.tables-section {
  row-gap: 20px;
}

.tables-section .el-col {
  margin-bottom: 0;
}
</style>
