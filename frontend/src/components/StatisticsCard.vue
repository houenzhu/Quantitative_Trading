<template>
  <el-card class="statistics-card" :class="{ 'highlight': highlight }" shadow="hover" :body-style="{ padding: '20px' }">
    <div class="card-content">
      <div class="card-header">
        <span class="card-title">{{ title }}</span>
        <el-icon class="card-icon" :size="20">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="card-value" :class="valueClass">{{ value }}</div>
      <div v-if="subtitle" class="card-subtitle">{{ subtitle }}</div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  icon: {
    type: String,
    default: 'Document'
  },
  subtitle: {
    type: String,
    default: ''
  },
  highlight: {
    type: Boolean,
    default: false
  },
  trend: {
    type: String,
    default: 'neutral'
  }
})

const valueClass = computed(() => {
  if (props.trend === 'up') return 'trend-up'
  if (props.trend === 'down') return 'trend-down'
  return ''
})
</script>

<style scoped>
.statistics-card {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.statistics-card.highlight {
  border-left: 3px solid #409eff;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 14px;
  color: #909399;
}

.card-icon {
  color: #409eff;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.card-value.trend-up {
  color: #f56c6c;
}

.card-value.trend-down {
  color: #67c23a;
}

.card-subtitle {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
