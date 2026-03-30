<template>
  <div class="strategy-container">
    <el-card class="header-card">
      <div class="header-content">
        <div class="title-section">
          <el-icon class="title-icon"><Operation /></el-icon>
          <h2>策略管理</h2>
        </div>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          创建策略
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="20" class="content-row">
      <el-col :span="16">
        <el-card class="strategy-list-card">
          <template #header>
            <div class="card-header">
              <span>我的策略</span>
              <el-tag>{{ strategies.length }} 个策略</el-tag>
            </div>
          </template>
          
          <el-table :data="strategies" style="width: 100%" v-loading="loading">
            <el-table-column prop="name" label="策略名称" min-width="150">
              <template #default="{ row }">
                <div class="strategy-name">
                  <span>{{ row.name }}</span>
                  <el-tag :type="getStatusType(row.status)" size="small">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="strategyType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag type="info">{{ getStrategyTypeName(row.strategyType) }}</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="统计" width="180">
              <template #default="{ row }">
                <div class="stats-info">
                  <span>信号: {{ row.totalSignals }}</span>
                  <span>胜率: {{ calculateWinRate(row) }}%</span>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="totalPnl" label="总盈亏" width="120">
              <template #default="{ row }">
                <span :class="row.totalPnl >= 0 ? 'profit' : 'loss'">
                  {{ formatMoney(row.totalPnl) }}
                </span>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button-group>
                  <el-button 
                    v-if="row.status !== 'active'" 
                    type="success" 
                    size="small"
                    @click="handleActivate(row)"
                  >
                    激活
                  </el-button>
                  <el-button 
                    v-if="row.status === 'active'" 
                    type="warning" 
                    size="small"
                    @click="handlePause(row)"
                  >
                    暂停
                  </el-button>
                  <el-button 
                    type="primary" 
                    size="small"
                    @click="showEditDialog(row)"
                  >
                    编辑
                  </el-button>
                  <el-button 
                    type="info" 
                    size="small"
                    @click="showDetailDialog(row)"
                  >
                    详情
                  </el-button>
                  <el-button 
                    type="danger" 
                    size="small"
                    @click="handleDelete(row)"
                  >
                    删除
                  </el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="signals-card">
          <template #header>
            <div class="card-header">
              <span>最近信号</span>
              <el-button text type="primary" @click="refreshSignals">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>
          
          <div class="signals-list" v-loading="signalsLoading">
            <div 
              v-for="signal in recentSignals" 
              :key="signal.id" 
              class="signal-item"
            >
              <div class="signal-header">
                <el-tag :type="signal.signalType === 'BUY' ? 'success' : 'danger'" size="small">
                  {{ signal.signalType === 'BUY' ? '买入' : '卖出' }}
                </el-tag>
                <span class="stock-code">{{ signal.stockCode }}</span>
              </div>
              <div class="signal-info">
                <div>{{ signal.stockName }}</div>
                <div class="signal-price">价格: ¥{{ signal.price }}</div>
              </div>
              <div class="signal-reason">{{ signal.reason }}</div>
              <div class="signal-time">{{ formatTime(signal.createdAt) }}</div>
            </div>
            
            <el-empty v-if="recentSignals.length === 0" description="暂无信号" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog 
      v-model="createDialogVisible" 
      :title="editMode ? '编辑策略' : '创建策略'" 
      width="600px"
    >
      <el-form :model="strategyForm" :rules="strategyRules" ref="strategyFormRef" label-width="120px">
        <el-form-item label="策略名称" prop="name">
          <el-input v-model="strategyForm.name" placeholder="请输入策略名称" />
        </el-form-item>
        
        <el-form-item label="策略描述">
          <el-input 
            v-model="strategyForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入策略描述"
          />
        </el-form-item>
        
        <el-form-item label="策略类型" prop="strategyType">
          <el-select 
            v-model="strategyForm.strategyType" 
            placeholder="请选择策略类型"
            @change="handleStrategyTypeChange"
            :disabled="editMode"
          >
            <el-option-group label="日K线策略">
              <el-option label="均线交叉策略" value="MA_CROSSOVER" />
              <el-option label="MACD策略" value="MACD" />
              <el-option label="RSI策略" value="RSI" />
              <el-option label="布林带策略" value="BOLLINGER" />
            </el-option-group>
            <el-option-group label="实时策略">
              <el-option label="动量策略 (实时)" value="MOMENTUM" />
              <el-option label="增强动量策略 (实时) ⭐推荐" value="ENHANCED_MOMENTUM" />
              <el-option label="VWAP策略 (实时)" value="VWAP" />
              <el-option label="复合策略 (实时)" value="COMPOSITE" />
            </el-option-group>
          </el-select>
        </el-form-item>
        
        <el-divider content-position="left">策略参数</el-divider>
        
        <template v-if="strategyForm.strategyType === 'MA_CROSSOVER'">
          <el-form-item label="短期均线">
            <el-input-number v-model="strategyForm.parameters.shortPeriod" :min="2" :max="60" />
          </el-form-item>
          <el-form-item label="长期均线">
            <el-input-number v-model="strategyForm.parameters.longPeriod" :min="5" :max="120" />
          </el-form-item>
          <el-form-item label="K线周期">
            <el-select v-model="strategyForm.parameters.klinePeriod">
              <el-option label="日线" value="1d" />
              <el-option label="60分钟" value="60m" />
              <el-option label="30分钟" value="30m" />
              <el-option label="15分钟" value="15m" />
              <el-option label="5分钟" value="5m" />
            </el-select>
          </el-form-item>
        </template>
        
        <template v-if="strategyForm.strategyType === 'MACD'">
          <el-form-item label="K线周期">
            <el-select v-model="strategyForm.parameters.klinePeriod">
              <el-option label="日线" value="1d" />
              <el-option label="60分钟" value="60m" />
              <el-option label="30分钟" value="30m" />
            </el-select>
          </el-form-item>
          <el-form-item label="要求零轴上方">
            <el-switch v-model="strategyForm.parameters.requireAboveZero" />
          </el-form-item>
        </template>
        
        <template v-if="strategyForm.strategyType === 'RSI'">
          <el-form-item label="RSI周期">
            <el-input-number v-model="strategyForm.parameters.period" :min="5" :max="30" />
          </el-form-item>
          <el-form-item label="超卖阈值">
            <el-input-number v-model="strategyForm.parameters.oversoldThreshold" :min="10" :max="40" />
          </el-form-item>
          <el-form-item label="超买阈值">
            <el-input-number v-model="strategyForm.parameters.overboughtThreshold" :min="60" :max="90" />
          </el-form-item>
          <el-form-item label="K线周期">
            <el-select v-model="strategyForm.parameters.klinePeriod">
              <el-option label="日线" value="1d" />
              <el-option label="60分钟" value="60m" />
              <el-option label="30分钟" value="30m" />
            </el-select>
          </el-form-item>
        </template>
        
        <template v-if="strategyForm.strategyType === 'BOLLINGER'">
          <el-form-item label="周期">
            <el-input-number v-model="strategyForm.parameters.period" :min="10" :max="30" />
          </el-form-item>
          <el-form-item label="标准差倍数">
            <el-input-number v-model="strategyForm.parameters.k" :min="1" :max="3" :step="0.1" :precision="1" />
          </el-form-item>
          <el-form-item label="K线周期">
            <el-select v-model="strategyForm.parameters.klinePeriod">
              <el-option label="日线" value="1d" />
              <el-option label="60分钟" value="60m" />
              <el-option label="30分钟" value="30m" />
            </el-select>
          </el-form-item>
        </template>
        
        <template v-if="strategyForm.strategyType === 'MOMENTUM'">
          <el-form-item label="回看周期">
            <el-input-number v-model="strategyForm.parameters.lookbackPeriod" :min="3" :max="200" />
            <span class="form-tip">分析最近N个tick数据（建议5-50）</span>
          </el-form-item>
          <el-form-item label="动量阈值">
            <el-input-number 
              v-model="strategyForm.parameters.momentumThreshold" 
              :min="0.001" 
              :max="0.1" 
              :step="0.001"
              :precision="3"
            />
            <span class="form-tip">价格变动超过此阈值才触发信号（如0.02表示2%）</span>
          </el-form-item>
        </template>
        
        <template v-if="strategyForm.strategyType === 'ENHANCED_MOMENTUM'">
          <el-alert 
            type="success" 
            :closable="false"
            style="margin-bottom: 15px;"
          >
            增强动量策略包含：趋势确认、成交量验证、波动率调整、最大回撤保护等多重风控机制
          </el-alert>
          <el-form-item label="回看周期">
            <el-input-number v-model="strategyForm.parameters.lookbackPeriod" :min="5" :max="50" />
            <span class="form-tip">动量计算周期（建议10-20）</span>
          </el-form-item>
          <el-form-item label="动量阈值">
            <el-input-number 
              v-model="strategyForm.parameters.momentumThreshold" 
              :min="0.001" 
              :max="0.05" 
              :step="0.001"
              :precision="3"
            />
            <span class="form-tip">基础动量阈值（会根据波动率自动调整）</span>
          </el-form-item>
          <el-form-item label="短期均线">
            <el-input-number v-model="strategyForm.parameters.shortMaPeriod" :min="3" :max="20" />
            <span class="form-tip">趋势确认的短期均线周期</span>
          </el-form-item>
          <el-form-item label="长期均线">
            <el-input-number v-model="strategyForm.parameters.longMaPeriod" :min="10" :max="50" />
            <span class="form-tip">趋势确认的长期均线周期</span>
          </el-form-item>
          <el-form-item label="成交量倍数">
            <el-input-number 
              v-model="strategyForm.parameters.volumeMultiplier" 
              :min="1" 
              :max="3" 
              :step="0.1"
              :precision="1"
            />
            <span class="form-tip">成交量需超过均值的倍数才算放量</span>
          </el-form-item>
          <el-form-item label="最大回撤限制">
            <el-input-number 
              v-model="strategyForm.parameters.maxDrawdownThreshold" 
              :min="0.01" 
              :max="0.1" 
              :step="0.01"
              :precision="2"
            />
            <span class="form-tip">超过此回撤将触发卖出（如0.03表示3%）</span>
          </el-form-item>
          <el-form-item label="趋势确认">
            <el-switch v-model="strategyForm.parameters.requireTrendConfirm" />
            <span class="form-tip">要求短期均线上穿长期均线且价格在短期均线上方</span>
          </el-form-item>
          <el-form-item label="成交量确认">
            <el-switch v-model="strategyForm.parameters.requireVolumeConfirm" />
            <span class="form-tip">要求成交量放大才算有效突破</span>
          </el-form-item>
        </template>
        
        <template v-if="strategyForm.strategyType === 'VWAP'">
          <el-form-item label="偏离阈值">
            <el-input-number 
              v-model="strategyForm.parameters.deviationThreshold" 
              :min="0.005" 
              :max="0.05" 
              :step="0.005"
              :precision="3"
            />
            <span class="form-tip">价格偏离VWAP超过此阈值才触发信号（如0.01表示1%）</span>
          </el-form-item>
        </template>
        
        <template v-if="strategyForm.strategyType === 'COMPOSITE'">
          <el-form-item label="动量阈值">
            <el-input-number 
              v-model="strategyForm.parameters.momentumThreshold" 
              :min="0.005" 
              :max="0.1" 
              :step="0.005"
              :precision="3"
            />
            <span class="form-tip">动量策略的触发阈值</span>
          </el-form-item>
          <el-form-item label="VWAP偏离阈值">
            <el-input-number 
              v-model="strategyForm.parameters.deviationThreshold" 
              :min="0.005" 
              :max="0.05" 
              :step="0.005"
              :precision="3"
            />
            <span class="form-tip">VWAP策略的偏离阈值</span>
          </el-form-item>
          <el-alert 
            type="info" 
            :closable="false"
            style="margin-bottom: 15px;"
          >
            复合策略需要动量和VWAP两个策略同时发出相同信号才会执行，更稳定但信号更少
          </el-alert>
        </template>
        
        <el-divider content-position="left">风控参数</el-divider>
        
        <el-form-item label="最大仓位比例">
          <el-input-number 
            v-model="strategyForm.maxPositionPct" 
            :min="0.01" 
            :max="1" 
            :step="0.01"
            :precision="2"
          />
          <span class="form-tip">单只股票最大仓位占总资金的比例</span>
        </el-form-item>
        
        <el-form-item label="止损比例">
          <el-input-number 
            v-model="strategyForm.stopLossPct" 
            :min="0.01" 
            :max="0.2" 
            :step="0.01"
            :precision="2"
          />
        </el-form-item>
        
        <el-form-item label="止盈比例">
          <el-input-number 
            v-model="strategyForm.takeProfitPct" 
            :min="0.01" 
            :max="0.5" 
            :step="0.01"
            :precision="2"
          />
        </el-form-item>
        
        <el-form-item label="每日最大交易次数">
          <el-input-number v-model="strategyForm.maxTradesPerDay" :min="1" :max="50" />
        </el-form-item>
        
        <el-form-item label="每次交易数量">
          <el-input-number v-model="strategyForm.tradeSize" :min="100" :max="10000" :step="100" />
        </el-form-item>
        
        <el-form-item label="自动执行">
          <el-switch v-model="strategyForm.autoExecute" />
          <span class="form-tip">开启后信号将自动下单执行</span>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ editMode ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="策略详情" width="700px">
      <div v-if="currentStrategy" class="strategy-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="策略名称">{{ currentStrategy.name }}</el-descriptions-item>
          <el-descriptions-item label="策略类型">
            <el-tag type="info">{{ getStrategyTypeName(currentStrategy.strategyType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentStrategy.status)">
              {{ getStatusText(currentStrategy.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatTime(currentStrategy.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="策略描述" :span="2">
            {{ currentStrategy.description || '暂无描述' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <el-divider content-position="left">统计数据</el-divider>
        
        <el-row :gutter="20" class="stats-row">
          <el-col :span="6">
            <el-statistic title="总信号数" :value="currentStrategy.totalSignals" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="已执行" :value="currentStrategy.executedSignals" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="盈利次数" :value="currentStrategy.winCount" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="亏损次数" :value="currentStrategy.lossCount" />
          </el-col>
        </el-row>
        
        <el-row :gutter="20" class="stats-row">
          <el-col :span="12">
            <el-statistic 
              title="总盈亏" 
              :value="currentStrategy.totalPnl"
              :precision="2"
              prefix="¥"
            />
          </el-col>
          <el-col :span="12">
            <el-statistic 
              title="胜率" 
              :value="calculateWinRate(currentStrategy)"
              suffix="%"
              :precision="2"
            />
          </el-col>
        </el-row>
        
        <el-divider content-position="left">策略信号历史</el-divider>
        
        <el-table :data="strategySignals" style="width: 100%" max-height="300">
          <el-table-column prop="signalType" label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="row.signalType === 'BUY' ? 'success' : 'danger'" size="small">
                {{ row.signalType === 'BUY' ? '买入' : '卖出' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="stockCode" label="股票代码" width="100" />
          <el-table-column prop="stockName" label="股票名称" width="120" />
          <el-table-column prop="price" label="价格" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getSignalStatusType(row.status)" size="small">
                {{ getSignalStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="150">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { strategyApi } from '@/api/strategy'

const stockPool = inject('stockPool')

const loading = ref(false)
const signalsLoading = ref(false)
const submitting = ref(false)
const strategies = ref([])
const recentSignals = ref([])
const strategySignals = ref([])
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const editMode = ref(false)
const currentStrategy = ref(null)
const strategyFormRef = ref(null)

const strategyForm = ref({
  name: '',
  description: '',
  strategyType: '',
  parameters: {},
  maxPositionPct: 0.1,
  stopLossPct: 0.05,
  takeProfitPct: 0.1,
  maxTradesPerDay: 10,
  tradeSize: 100,
  autoExecute: false
})

const strategyRules = {
  name: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  strategyType: [{ required: true, message: '请选择策略类型', trigger: 'change' }]
}

const loadStrategies = async () => {
  loading.value = true
  try {
    const res = await strategyApi.getUserStrategies()
    if (res.code === 200) {
      strategies.value = res.data || []
    }
  } catch (e) {
    console.error('加载策略列表失败', e)
    ElMessage.error('加载策略列表失败')
  } finally {
    loading.value = false
  }
}

const loadRecentSignals = async () => {
  signalsLoading.value = true
  try {
    const res = await strategyApi.getRecentSignals(20)
    if (res.code === 200) {
      recentSignals.value = res.data || []
    }
  } catch (e) {
    console.error('加载信号失败', e)
  } finally {
    signalsLoading.value = false
  }
}

const refreshSignals = () => {
  loadRecentSignals()
}

const showCreateDialog = () => {
  editMode.value = false
  strategyForm.value = {
    name: '',
    description: '',
    strategyType: '',
    parameters: {},
    maxPositionPct: 0.1,
    stopLossPct: 0.05,
    takeProfitPct: 0.1,
    maxTradesPerDay: 10,
    tradeSize: 100,
    autoExecute: false
  }
  createDialogVisible.value = true
}

const showEditDialog = (strategy) => {
  editMode.value = true
  currentStrategy.value = strategy
  
  try {
    const params = JSON.parse(strategy.parameters || '{}')
    strategyForm.value = {
      name: strategy.name,
      description: strategy.description || '',
      strategyType: strategy.strategyType,
      parameters: { ...params },
      maxPositionPct: strategy.maxPositionPct,
      stopLossPct: strategy.stopLossPct,
      takeProfitPct: strategy.takeProfitPct,
      maxTradesPerDay: strategy.maxTradesPerDay,
      tradeSize: strategy.tradeSize,
      autoExecute: strategy.autoExecute
    }
  } catch (e) {
    console.error('解析策略参数失败', e)
  }
  
  createDialogVisible.value = true
}

const showDetailDialog = async (strategy) => {
  currentStrategy.value = strategy
  detailDialogVisible.value = true
  
  try {
    const res = await strategyApi.getStrategySignals(strategy.id, 20)
    if (res.code === 200) {
      strategySignals.value = res.data || []
    }
  } catch (e) {
    console.error('加载策略信号失败', e)
  }
}

const handleStrategyTypeChange = async (type) => {
  if (editMode.value) {
    console.log('编辑模式，不加载默认参数')
    return
  }
  try {
    const res = await strategyApi.getDefaultParameters(type)
    if (res.code === 200) {
      console.log('加载默认参数:', res.data)
      strategyForm.value.parameters = { ...res.data } || {}
    }
  } catch (e) {
    console.error('获取默认参数失败', e)
  }
}

const handleSubmit = async () => {
  if (!strategyFormRef.value) return
  
  await strategyFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      if (editMode.value) {
        const updates = {
          name: strategyForm.value.name,
          description: strategyForm.value.description,
          parameters: strategyForm.value.parameters,
          maxPositionPct: strategyForm.value.maxPositionPct,
          stopLossPct: strategyForm.value.stopLossPct,
          takeProfitPct: strategyForm.value.takeProfitPct,
          maxTradesPerDay: strategyForm.value.maxTradesPerDay,
          tradeSize: strategyForm.value.tradeSize,
          autoExecute: strategyForm.value.autoExecute
        }
        
        const res = await strategyApi.updateStrategy(currentStrategy.value.id, updates)
        if (res.code === 200) {
          ElMessage.success('策略更新成功')
          createDialogVisible.value = false
          loadStrategies()
        } else {
          ElMessage.error(res.message || '更新失败')
        }
      } else {
        const res = await strategyApi.createStrategy(strategyForm.value)
        if (res.code === 200) {
          ElMessage.success('策略创建成功')
          createDialogVisible.value = false
          loadStrategies()
        } else {
          ElMessage.error(res.message || '创建失败')
        }
      }
    } catch (e) {
      console.error('提交失败', e)
      ElMessage.error('操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleActivate = async (strategy) => {
  try {
    const res = await strategyApi.activateStrategy(strategy.id)
    if (res.code === 200) {
      ElMessage.success('策略已激活')
      loadStrategies()
    } else {
      ElMessage.error(res.message || '激活失败')
    }
  } catch (e) {
    console.error('激活失败', e)
    ElMessage.error('激活失败')
  }
}

const handlePause = async (strategy) => {
  try {
    const res = await strategyApi.pauseStrategy(strategy.id)
    if (res.code === 200) {
      ElMessage.success('策略已暂停')
      loadStrategies()
    } else {
      ElMessage.error(res.message || '暂停失败')
    }
  } catch (e) {
    console.error('暂停失败', e)
    ElMessage.error('暂停失败')
  }
}

const handleDelete = async (strategy) => {
  try {
    await ElMessageBox.confirm('确定要删除该策略吗？删除后无法恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await strategyApi.deleteStrategy(strategy.id)
    if (res.code === 200) {
      ElMessage.success('策略已删除')
      loadStrategies()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
      ElMessage.error('删除失败')
    }
  }
}

const getStatusType = (status) => {
  const types = {
    'active': 'success',
    'inactive': 'info',
    'paused': 'warning'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'active': '运行中',
    'inactive': '未激活',
    'paused': '已暂停'
  }
  return texts[status] || status
}

const getStrategyTypeName = (type) => {
  const names = {
    'MA_CROSSOVER': '均线交叉',
    'MACD': 'MACD',
    'RSI': 'RSI',
    'BOLLINGER': '布林带',
    'MOMENTUM': '动量策略',
    'VWAP': 'VWAP策略',
    'COMPOSITE': '复合策略'
  }
  return names[type] || type
}

const getSignalStatusType = (status) => {
  const types = {
    'pending': 'warning',
    'executed': 'success',
    'cancelled': 'info',
    'expired': 'danger'
  }
  return types[status] || 'info'
}

const getSignalStatusText = (status) => {
  const texts = {
    'pending': '待执行',
    'executed': '已执行',
    'cancelled': '已取消',
    'expired': '已过期'
  }
  return texts[status] || status
}

const calculateWinRate = (strategy) => {
  const total = (strategy.winCount || 0) + (strategy.lossCount || 0)
  if (total === 0) return 0
  return ((strategy.winCount || 0) / total * 100).toFixed(2)
}

const formatMoney = (value) => {
  if (!value) return '¥0.00'
  const num = Number(value)
  return (num >= 0 ? '¥' : '-¥') + Math.abs(num).toFixed(2)
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  loadStrategies()
  loadRecentSignals()
})
</script>

<style scoped>
.strategy-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  font-size: 24px;
  color: #409eff;
}

.title-section h2 {
  margin: 0;
  font-size: 20px;
}

.content-row {
  min-height: calc(100vh - 200px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.strategy-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stats-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #606266;
}

.profit {
  color: #67c23a;
  font-weight: 600;
}

.loss {
  color: #f56c6c;
  font-weight: 600;
}

.signals-card {
  height: 100%;
}

.signals-list {
  max-height: 600px;
  overflow-y: auto;
}

.signal-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.signal-item:hover {
  background-color: #f5f7fa;
}

.signal-item:last-child {
  border-bottom: none;
}

.signal-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.stock-code {
  font-weight: 600;
  color: #303133;
}

.signal-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 13px;
}

.signal-price {
  color: #909399;
}

.signal-reason {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.signal-time {
  font-size: 11px;
  color: #909399;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.strategy-detail {
  padding: 10px 0;
}

.stats-row {
  margin-bottom: 20px;
}
</style>
