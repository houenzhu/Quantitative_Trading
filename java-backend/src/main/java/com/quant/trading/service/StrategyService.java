package com.quant.trading.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.trading.entity.SignalEntity;
import com.quant.trading.entity.Strategy;
import com.quant.trading.mapper.SignalMapper;
import com.quant.trading.mapper.StrategyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略管理服务
 * 提供策略的CRUD操作和业务逻辑
 */
@Service
@Slf4j
public class StrategyService {
    
    @Autowired
    private StrategyMapper strategyMapper;
    
    @Autowired
    private SignalMapper signalMapper;
    
    @Autowired
    private StrategyEngineService strategyEngineService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 创建新策略
     * @param userId 用户ID
     * @param name 策略名称
     * @param description 策略描述
     * @param strategyType 策略类型
     * @param parameters 策略参数
     * @return 创建的策略对象
     */
    @Transactional
    public Strategy createStrategy(Long userId, String name, String description, 
                                    String strategyType, Map<String, Object> parameters) {
        try {
            Strategy strategy = new Strategy();
            strategy.setUserId(userId);
            strategy.setName(name);
            strategy.setDescription(description);
            strategy.setStrategyType(strategyType);
            strategy.setStatus("inactive");
            strategy.setParameters(objectMapper.writeValueAsString(parameters));
            
            strategy.setMaxPositionPct(new BigDecimal("0.1"));
            strategy.setStopLossPct(new BigDecimal("0.05"));
            strategy.setTakeProfitPct(new BigDecimal("0.1"));
            strategy.setMaxTradesPerDay(10);
            strategy.setTradeSize(100);
            strategy.setAllowShort(false);
            strategy.setAutoExecute(false);
            
            strategy.setTotalSignals(0);
            strategy.setExecutedSignals(0);
            strategy.setWinCount(0);
            strategy.setLossCount(0);
            strategy.setTotalPnl(BigDecimal.ZERO);
            
            strategyMapper.insert(strategy);
            log.info("创建策略成功: {} (ID: {})", name, strategy.getId());
            
            return strategy;
        } catch (Exception e) {
            log.error("创建策略失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新策略配置
     * @param strategyId 策略ID
     * @param updates 更新的字段
     * @return 更新后的策略
     */
    @Transactional
    public Strategy updateStrategy(Long strategyId, Map<String, Object> updates) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RuntimeException("策略不存在: " + strategyId);
        }
        
        try {
            if (updates.containsKey("name")) {
                strategy.setName((String) updates.get("name"));
            }
            if (updates.containsKey("description")) {
                strategy.setDescription((String) updates.get("description"));
            }
            if (updates.containsKey("parameters")) {
                strategy.setParameters(objectMapper.writeValueAsString(updates.get("parameters")));
            }
            if (updates.containsKey("maxPositionPct")) {
                strategy.setMaxPositionPct(new BigDecimal(updates.get("maxPositionPct").toString()));
            }
            if (updates.containsKey("stopLossPct")) {
                strategy.setStopLossPct(new BigDecimal(updates.get("stopLossPct").toString()));
            }
            if (updates.containsKey("takeProfitPct")) {
                strategy.setTakeProfitPct(new BigDecimal(updates.get("takeProfitPct").toString()));
            }
            if (updates.containsKey("maxTradesPerDay")) {
                strategy.setMaxTradesPerDay((Integer) updates.get("maxTradesPerDay"));
            }
            if (updates.containsKey("tradeSize")) {
                strategy.setTradeSize((Integer) updates.get("tradeSize"));
            }
            if (updates.containsKey("allowShort")) {
                strategy.setAllowShort((Boolean) updates.get("allowShort"));
            }
            if (updates.containsKey("autoExecute")) {
                strategy.setAutoExecute((Boolean) updates.get("autoExecute"));
            }
            
            strategyMapper.updateById(strategy);
            log.info("更新策略成功: {}", strategy.getName());
            
            return strategy;
        } catch (Exception e) {
            log.error("更新策略失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 激活策略
     * @param strategyId 策略ID
     * @return 是否成功
     */
    @Transactional
    public boolean activateStrategy(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RuntimeException("策略不存在: " + strategyId);
        }
        
        strategy.setStatus("active");
        strategyMapper.updateById(strategy);
        log.info("激活策略: {}", strategy.getName());
        return true;
    }
    
    /**
     * 暂停策略
     * @param strategyId 策略ID
     * @return 是否成功
     */
    @Transactional
    public boolean pauseStrategy(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RuntimeException("策略不存在: " + strategyId);
        }
        
        strategy.setStatus("paused");
        strategyMapper.updateById(strategy);
        log.info("暂停策略: {}", strategy.getName());
        return true;
    }
    
    /**
     * 停用策略
     * @param strategyId 策略ID
     * @return 是否成功
     */
    @Transactional
    public boolean deactivateStrategy(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RuntimeException("策略不存在: " + strategyId);
        }
        
        strategy.setStatus("inactive");
        strategyMapper.updateById(strategy);
        log.info("停用策略: {}", strategy.getName());
        return true;
    }
    
    /**
     * 删除策略
     * @param strategyId 策略ID
     * @return 是否成功
     */
    @Transactional
    public boolean deleteStrategy(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RuntimeException("策略不存在: " + strategyId);
        }
        
        strategyMapper.deleteById(strategyId);
        log.info("删除策略: {}", strategy.getName());
        return true;
    }
    
    /**
     * 获取用户的所有策略
     * @param userId 用户ID
     * @return 策略列表
     */
    public List<Strategy> getUserStrategies(Long userId) {
        return strategyMapper.findByUserId(userId);
    }
    
    /**
     * 获取策略详情
     * @param strategyId 策略ID
     * @return 策略对象
     */
    public Strategy getStrategyById(Long strategyId) {
        return strategyMapper.selectById(strategyId);
    }
    
    /**
     * 手动执行策略
     * @param strategyId 策略ID
     * @param stockPool 股票池
     * @return 生成的信号列表
     */
    public List<SignalEntity> executeStrategyManually(Long strategyId, Map<String, String> stockPool) {
        return strategyEngineService.executeStrategy(strategyId, stockPool);
    }
    
    /**
     * 获取策略的信号历史
     * @param strategyId 策略ID
     * @param limit 限制数量
     * @return 信号列表
     */
    public List<SignalEntity> getStrategySignals(Long strategyId, int limit) {
        return signalMapper.findByStrategyId(strategyId, limit);
    }
    
    /**
     * 获取策略统计数据
     * @param strategyId 策略ID
     * @return 统计数据
     */
    public Map<String, Object> getStrategyStatistics(Long strategyId) {
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RuntimeException("策略不存在: " + strategyId);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSignals", strategy.getTotalSignals());
        stats.put("executedSignals", strategy.getExecutedSignals());
        stats.put("winCount", strategy.getWinCount());
        stats.put("lossCount", strategy.getLossCount());
        stats.put("totalPnl", strategy.getTotalPnl());
        
        int total = strategy.getWinCount() + strategy.getLossCount();
        if (total > 0) {
            stats.put("winRate", (double) strategy.getWinCount() / total * 100);
        } else {
            stats.put("winRate", 0.0);
        }
        
        if (strategy.getExecutedSignals() > 0) {
            stats.put("executionRate", (double) strategy.getExecutedSignals() / strategy.getTotalSignals() * 100);
        } else {
            stats.put("executionRate", 0.0);
        }
        
        return stats;
    }
    
    /**
     * 获取用户的待执行信号
     * @param userId 用户ID
     * @return 待执行信号列表
     */
    public List<SignalEntity> getPendingSignals(Long userId) {
        return signalMapper.findPendingSignals(userId);
    }
    
    /**
     * 获取用户的最近信号
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 信号列表
     */
    public List<SignalEntity> getRecentSignals(Long userId, int limit) {
        return signalMapper.findByUserIdOrderByCreatedAtDesc(userId, limit);
    }
    
    /**
     * 获取默认策略参数模板
     * @param strategyType 策略类型
     * @return 参数模板
     */
    public Map<String, Object> getDefaultParameters(String strategyType) {
        Map<String, Object> params = new HashMap<>();
        
        switch (strategyType) {
            case "MA_CROSSOVER":
                params.put("shortPeriod", 5);
                params.put("longPeriod", 20);
                params.put("klinePeriod", "1d");
                break;
            case "MACD":
                params.put("klinePeriod", "1d");
                params.put("requireAboveZero", true);
                break;
            case "RSI":
                params.put("period", 14);
                params.put("klinePeriod", "1d");
                params.put("oversoldThreshold", 30);
                params.put("overboughtThreshold", 70);
                break;
            case "BOLLINGER":
                params.put("period", 20);
                params.put("k", 2.0);
                params.put("klinePeriod", "1d");
                break;
            case "MOMENTUM":
                params.put("lookbackPeriod", 5);
                params.put("momentumThreshold", 0.02);
                break;
            case "ENHANCED_MOMENTUM":
                params.put("lookbackPeriod", 15);
                params.put("momentumThreshold", 0.004);
                params.put("shortMaPeriod", 5);
                params.put("longMaPeriod", 20);
                params.put("volumeMultiplier", 1.5);
                params.put("maxDrawdownThreshold", 0.03);
                params.put("requireTrendConfirm", true);
                params.put("requireVolumeConfirm", true);
                break;
            case "VWAP":
                params.put("deviationThreshold", 0.01);
                break;
            case "COMPOSITE":
                params.put("momentumThreshold", 0.02);
                params.put("deviationThreshold", 0.01);
                break;
            default:
                break;
        }
        
        return params;
    }
}
