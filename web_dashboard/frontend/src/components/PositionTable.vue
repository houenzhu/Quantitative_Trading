<template>
  <el-card class="position-table" shadow="hover">
    <template #header>
      <div class="card-header">
        <div class="header-left">
          <el-icon><Briefcase /></el-icon>
          <span>持仓明细</span>
        </div>
        <el-tag size="small" type="info" effect="plain">{{ positions.length }} 只</el-tag>
      </div>
    </template>
    <el-table
      :data="positions"
      style="width: 100%"
      empty-text="暂无持仓"
      stripe
    >
      <el-table-column prop="stock_code" label="代码" width="90" />
      <el-table-column prop="stock_name" label="名称" width="100" />
      <el-table-column prop="quantity" label="持仓数量" width="100" align="right" />
      <el-table-column prop="avg_price" label="成本价" width="100" align="right">
        <template #default="{ row }">
          ¥{{ row.avg_price.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="current_price" label="现价" width="100" align="right">
        <template #default="{ row }">
          ¥{{ row.current_price.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="market_value" label="市值" width="120" align="right">
        <template #default="{ row }">
          ¥{{ row.market_value.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="profit_loss" label="盈亏" width="120" align="right">
        <template #default="{ row }">
          <el-text :type="row.profit_loss >= 0 ? 'danger' : 'success'" tag="b">
            {{ row.profit_loss >= 0 ? '+' : '' }}¥{{ row.profit_loss.toFixed(2) }}
          </el-text>
        </template>
      </el-table-column>
      <el-table-column prop="profit_loss_pct" label="收益率" align="right">
        <template #default="{ row }">
          <el-text :type="row.profit_loss_pct >= 0 ? 'danger' : 'success'" tag="b">
            {{ row.profit_loss_pct >= 0 ? '+' : '' }}{{ row.profit_loss_pct.toFixed(2) }}%
          </el-text>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { inject } from 'vue'

const positions = inject('positions')
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
