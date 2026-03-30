<template>
  <el-card class="order-table" shadow="hover">
    <template #header>
      <div class="card-header">
        <div class="header-left">
          <el-icon><List /></el-icon>
          <span>订单记录</span>
        </div>
        <el-tag size="small" type="info" effect="plain">最近 {{ displayOrders.length }} 条</el-tag>
      </div>
    </template>
    <el-table
      :data="displayOrders"
      style="width: 100%"
      empty-text="暂无订单"
      stripe
      max-height="400"
    >
      <el-table-column prop="order_id" label="订单号" width="180" />
      <el-table-column prop="stock_code" label="代码" width="80" />
      <el-table-column prop="stock_name" label="名称" width="90" />
      <el-table-column prop="side" label="方向" width="70">
        <template #default="{ row }">
          <el-tag :type="row.side === '买入' || row.side === 'BUY' ? 'danger' : 'success'" size="small" effect="plain">
            {{ row.side === 'BUY' ? '买入' : (row.side === 'SELL' ? '卖出' : row.side) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="80" align="right" />
      <el-table-column prop="price" label="委托价" width="90" align="right">
        <template #default="{ row }">
          ¥{{ formatNumber(row.price, 2) }}
        </template>
      </el-table-column>
      <el-table-column prop="executed_price" label="成交价" width="90" align="right">
        <template #default="{ row }">
          <span v-if="row.executed_price">¥{{ formatNumber(row.executed_price, 2) }}</span>
          <el-text v-else type="info">--</el-text>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small" effect="plain">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="时间" width="160" />
      <el-table-column prop="reason" label="原因" show-overflow-tooltip />
    </el-table>
  </el-card>
</template>

<script setup>
import { inject, computed } from 'vue'

const orders = inject('orders')

const displayOrders = computed(() => {
  if (!orders || !orders.value) return []
  return orders.value.filter(o => o != null)
})

const formatNumber = (value, decimals = 2) => {
  if (value === null || value === undefined) return '0.00'
  const num = Number(value)
  if (isNaN(num)) return '0.00'
  return num.toFixed(decimals)
}

const getStatusType = (status) => {
  switch (status) {
    case '已成交':
    case 'filled':
      return 'success'
    case '待执行':
    case 'pending':
      return 'warning'
    case '已撤销':
    case 'cancelled':
      return 'info'
    case '已拒绝':
    case 'rejected':
      return 'danger'
    default:
      return ''
  }
}
</script>

<style scoped>
.order-table {
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
