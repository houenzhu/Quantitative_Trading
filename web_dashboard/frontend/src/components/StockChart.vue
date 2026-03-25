<template>
  <el-card class="stock-chart" shadow="hover" :body-style="{ padding: '16px' }">
    <div class="chart-header">
      <div class="stock-info">
        <span class="stock-name">{{ stockName }}</span>
        <el-tag size="small" type="info" effect="plain">{{ stockCode }}</el-tag>
      </div>
      <div class="price-info">
        <span class="current-price" :class="priceClass">{{ currentPrice }}</span>
        <el-tag 
          :type="priceTagType" 
          size="small"
          effect="plain"
        >
          {{ changePercentText }}
        </el-tag>
      </div>
    </div>
    <div ref="chartRef" class="chart-container"></div>
  </el-card>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, inject } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  stockCode: {
    type: String,
    required: true
  },
  stockName: {
    type: String,
    required: true
  }
})

const chartRef = ref(null)
let chart = null

const tickHistory = inject('tickHistory')
const stockData = inject('stockData')

const currentPrice = computed(() => {
  const data = stockData[props.stockCode]
  return data?.price ? `¥${data.price.toFixed(2)}` : '--'
})

const changePercent = computed(() => {
  const data = stockData[props.stockCode]
  return data?.change_percent || 0
})

const changePercentText = computed(() => {
  const pct = changePercent.value
  const sign = pct >= 0 ? '+' : ''
  return `${sign}${pct.toFixed(2)}%`
})

const priceClass = computed(() => {
  const pct = changePercent.value
  if (pct > 0) return 'price-up'
  if (pct < 0) return 'price-down'
  return 'price-flat'
})

const priceTagType = computed(() => {
  const pct = changePercent.value
  if (pct > 0) return 'danger'
  if (pct < 0) return 'success'
  return 'info'
})

const initChart = () => {
  if (!chartRef.value) return
  
  chart = echarts.init(chartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e4e7ed',
      textStyle: {
        color: '#303133'
      },
      formatter: (params) => {
        if (!params || !params[0]) return ''
        const data = params[0].data
        return `
          <div style="padding: 8px;">
            <div style="color: #909399;">时间: ${params[0].axisValue}</div>
            <div style="color: #303133; font-weight: 600;">价格: ¥${data[1]?.toFixed(2) || '--'}</div>
            <div style="color: #606266;">涨跌幅: ${data[2]?.toFixed(2) || 0}%</div>
          </div>
        `
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: [],
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      },
      axisLabel: {
        color: '#909399',
        fontSize: 10
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      },
      axisLabel: {
        color: '#909399',
        fontSize: 10,
        formatter: '¥{value}'
      },
      splitLine: {
        lineStyle: {
          color: '#ebeef5'
        }
      }
    },
    series: [
      {
        name: '价格',
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: {
          width: 2,
          color: '#409eff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.25)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
          ])
        },
        data: []
      }
    ]
  }
  
  chart.setOption(option)
}

const updateChart = () => {
  if (!chart) return
  
  const history = tickHistory[props.stockCode] || []
  
  if (history.length === 0) return
  
  const times = history.map(item => {
    if (item.timestamp) {
      const date = new Date(item.timestamp)
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    }
    return ''
  })
  
  const prices = history.map(item => [
    times[history.indexOf(item)],
    item.price,
    item.change_percent
  ])
  
  chart.setOption({
    xAxis: {
      data: times
    },
    series: [{
      data: prices
    }]
  })
}

watch(
  () => tickHistory[props.stockCode],
  () => {
    updateChart()
  },
  { deep: true }
)

const handleResize = () => {
  if (chart) {
    chart.resize()
  }
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chart) {
    chart.dispose()
    chart = null
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.stock-chart {
  height: 280px;
  border-radius: 8px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.stock-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stock-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.price-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-price {
  font-size: 16px;
  font-weight: 700;
}

.price-up {
  color: #f56c6c;
}

.price-down {
  color: #67c23a;
}

.price-flat {
  color: #909399;
}

.chart-container {
  height: 180px;
}
</style>
