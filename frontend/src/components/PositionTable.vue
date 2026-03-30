<template>
  <el-card class="position-table" shadow="hover">
    <template #header>
      <div class="card-header">
        <div class="header-left">
          <el-icon><Briefcase /></el-icon>
          <span>持仓明细</span>
        </div>
        <el-tag size="small" type="info" effect="plain">{{ displayPositions.length }} 只</el-tag>
      </div>
    </template>
    <el-table
      :data="displayPositions"
      style="width: 100%"
      empty-text="暂无持仓"
      stripe
    >
      <el-table-column prop="stock_code" label="代码" width="90" />
      <el-table-column prop="stock_name" label="名称" width="100" />
      <el-table-column prop="quantity" label="持仓数量" width="100" align="right" />
      <el-table-column prop="avg_price" label="成本价" width="100" align="right">
        <template #default="{ row }">
          ¥{{ formatNumber(row.avg_price, 2) }}
        </template>
      </el-table-column>
      <el-table-column prop="current_price" label="现价" width="100" align="right">
        <template #default="{ row }">
          ¥{{ formatNumber(row.current_price, 2) }}
        </template>
      </el-table-column>
      <el-table-column prop="market_value" label="市值" width="120" align="right">
        <template #default="{ row }">
          ¥{{ formatNumber(row.market_value, 2) }}
        </template>
      </el-table-column>
      <el-table-column prop="profit_loss" label="盈亏" width="120" align="right">
        <template #default="{ row }">
          <el-text :type="getProfitType(row.profit_loss)" tag="b">
            {{ row.profit_loss >= 0 ? '+' : '' }}¥{{ formatNumber(row.profit_loss, 2) }}
          </el-text>
        </template>
      </el-table-column>
      <el-table-column prop="profit_loss_pct" label="收益率" align="right">
        <template #default="{ row }">
          <el-text :type="getProfitType(row.profit_loss_pct)" tag="b">
            {{ row.profit_loss_pct >= 0 ? '+' : '' }}{{ formatNumber(row.profit_loss_pct, 2) }}%
          </el-text>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { inject, computed } from 'vue'

const positions = inject('positions')

const displayPositions = computed(() => {
  if (!positions || !positions.value) return []
  return positions.value.filter(p => p && p.quantity > 0)
})

const formatNumber = (value, decimals = 2) => {
  if (value === null || value === undefined) return '0.00'
  const num = Number(value)
  if (isNaN(num)) return '0.00'
  return num.toFixed(decimals)
}

const getProfitType = (value) => {
  const num = Number(value)
  if (isNaN(num) || num === 0) return ''
  return num > 0 ? 'danger' : 'success'
}
</script>

<style scoped>
.position-table {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.header-left .el-icon {
  color: #409eff;
}
</style>
