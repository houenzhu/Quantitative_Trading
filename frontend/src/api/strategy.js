import request from '@/utils/request'

/**
 * 策略管理API
 */

export const strategyApi = {
  /**
   * 创建新策略
   * @param {Object} data - 策略数据
   * @param {string} data.name - 策略名称
   * @param {string} data.description - 策略描述
   * @param {string} data.strategyType - 策略类型
   * @param {Object} data.parameters - 策略参数
   */
  createStrategy(data) {
    return request.post('/api/strategy/create', data)
  },

  /**
   * 更新策略配置
   * @param {number} strategyId - 策略ID
   * @param {Object} updates - 更新的字段
   */
  updateStrategy(strategyId, updates) {
    return request.put(`/api/strategy/${strategyId}`, updates)
  },

  /**
   * 激活策略
   * @param {number} strategyId - 策略ID
   */
  activateStrategy(strategyId) {
    return request.post(`/api/strategy/${strategyId}/activate`)
  },

  /**
   * 暂停策略
   * @param {number} strategyId - 策略ID
   */
  pauseStrategy(strategyId) {
    return request.post(`/api/strategy/${strategyId}/pause`)
  },

  /**
   * 停用策略
   * @param {number} strategyId - 策略ID
   */
  deactivateStrategy(strategyId) {
    return request.post(`/api/strategy/${strategyId}/deactivate`)
  },

  /**
   * 删除策略
   * @param {number} strategyId - 策略ID
   */
  deleteStrategy(strategyId) {
    return request.delete(`/api/strategy/${strategyId}`)
  },

  /**
   * 获取用户的所有策略
   */
  getUserStrategies() {
    return request.get('/api/strategy/list')
  },

  /**
   * 获取策略详情
   * @param {number} strategyId - 策略ID
   */
  getStrategyDetail(strategyId) {
    return request.get(`/api/strategy/${strategyId}`)
  },

  /**
   * 手动执行策略
   * @param {number} strategyId - 策略ID
   * @param {Object} stockPool - 股票池
   */
  executeStrategy(strategyId, stockPool) {
    return request.post(`/api/strategy/${strategyId}/execute`, stockPool)
  },

  /**
   * 获取策略统计数据
   * @param {number} strategyId - 策略ID
   */
  getStrategyStatistics(strategyId) {
    return request.get(`/api/strategy/${strategyId}/statistics`)
  },

  /**
   * 获取策略的信号历史
   * @param {number} strategyId - 策略ID
   * @param {number} limit - 限制数量
   */
  getStrategySignals(strategyId, limit = 50) {
    return request.get(`/api/strategy/${strategyId}/signals`, { params: { limit } })
  },

  /**
   * 获取用户的待执行信号
   */
  getPendingSignals() {
    return request.get('/api/strategy/signals/pending')
  },

  /**
   * 获取用户的最近信号
   * @param {number} limit - 限制数量
   */
  getRecentSignals(limit = 50) {
    return request.get('/api/strategy/signals/recent', { params: { limit } })
  },

  /**
   * 获取策略类型的默认参数
   * @param {string} strategyType - 策略类型
   */
  getDefaultParameters(strategyType) {
    return request.get('/api/strategy/parameters/default', { params: { strategyType } })
  }
}
