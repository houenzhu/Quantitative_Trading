package com.quant.trading.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.quant.trading.common.Result;
import com.quant.trading.entity.SignalEntity;
import com.quant.trading.entity.Strategy;
import com.quant.trading.service.StrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 策略管理控制器
 * 提供策略的CRUD、执行和查询接口
 */
@RestController
@RequestMapping("/api/strategy")
@Slf4j
public class StrategyController {
    
    @Autowired
    private StrategyService strategyService;
    
    /**
     * 创建新策略
     * @param request 请求参数
     * @return 创建的策略
     */
    @PostMapping("/create")
    public Result<Strategy> createStrategy(@RequestBody Map<String, Object> request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String strategyType = (String) request.get("strategyType");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
            
            if (name == null || name.trim().isEmpty()) {
                return Result.error("策略名称不能为空");
            }
            if (strategyType == null || strategyType.trim().isEmpty()) {
                return Result.error("策略类型不能为空");
            }
            
            Strategy strategy = strategyService.createStrategy(userId, name, description, strategyType, parameters);
            return Result.success(strategy);
        } catch (Exception e) {
            log.error("创建策略失败: {}", e.getMessage(), e);
            return Result.error("创建策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新策略配置
     * @param strategyId 策略ID
     * @param updates 更新的字段
     * @return 更新后的策略
     */
    @PutMapping("/{strategyId}")
    public Result<Strategy> updateStrategy(@PathVariable Long strategyId, 
                                            @RequestBody Map<String, Object> updates) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Strategy strategy = strategyService.getStrategyById(strategyId);
            
            if (strategy == null) {
                return Result.error("策略不存在");
            }
            
            if (!strategy.getUserId().equals(userId)) {
                return Result.error("无权修改此策略");
            }
            
            Strategy updated = strategyService.updateStrategy(strategyId, updates);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新策略失败: {}", e.getMessage(), e);
            return Result.error("更新策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 激活策略
     * @param strategyId 策略ID
     * @return 操作结果
     */
    @PostMapping("/{strategyId}/activate")
    public Result<String> activateStrategy(@PathVariable Long strategyId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Strategy strategy = strategyService.getStrategyById(strategyId);
            
            if (strategy == null) {
                return Result.error("策略不存在");
            }
            
            if (!strategy.getUserId().equals(userId)) {
                return Result.error("无权操作此策略");
            }
            
            strategyService.activateStrategy(strategyId);
            return Result.success("策略已激活");
        } catch (Exception e) {
            log.error("激活策略失败: {}", e.getMessage(), e);
            return Result.error("激活策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 暂停策略
     * @param strategyId 策略ID
     * @return 操作结果
     */
    @PostMapping("/{strategyId}/pause")
    public Result<String> pauseStrategy(@PathVariable Long strategyId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Strategy strategy = strategyService.getStrategyById(strategyId);
            
            if (strategy == null) {
                return Result.error("策略不存在");
            }
            
            if (!strategy.getUserId().equals(userId)) {
                return Result.error("无权操作此策略");
            }
            
            strategyService.pauseStrategy(strategyId);
            return Result.success("策略已暂停");
        } catch (Exception e) {
            log.error("暂停策略失败: {}", e.getMessage(), e);
            return Result.error("暂停策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 停用策略
     * @param strategyId 策略ID
     * @return 操作结果
     */
    @PostMapping("/{strategyId}/deactivate")
    public Result<String> deactivateStrategy(@PathVariable Long strategyId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Strategy strategy = strategyService.getStrategyById(strategyId);
            
            if (strategy == null) {
                return Result.error("策略不存在");
            }
            
            if (!strategy.getUserId().equals(userId)) {
                return Result.error("无权操作此策略");
            }
            
            strategyService.deactivateStrategy(strategyId);
            return Result.success("策略已停用");
        } catch (Exception e) {
            log.error("停用策略失败: {}", e.getMessage(), e);
            return Result.error("停用策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除策略
     * @param strategyId 策略ID
     * @return 操作结果
     */
    @DeleteMapping("/{strategyId}")
    public Result<String> deleteStrategy(@PathVariable Long strategyId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Strategy strategy = strategyService.getStrategyById(strategyId);
            
            if (strategy == null) {
                return Result.error("策略不存在");
            }
            
            if (!strategy.getUserId().equals(userId)) {
                return Result.error("无权删除此策略");
            }
            
            strategyService.deleteStrategy(strategyId);
            return Result.success("策略已删除");
        } catch (Exception e) {
            log.error("删除策略失败: {}", e.getMessage(), e);
            return Result.error("删除策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的所有策略
     * @return 策略列表
     */
    @GetMapping("/list")
    public Result<List<Strategy>> getUserStrategies() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            List<Strategy> strategies = strategyService.getUserStrategies(userId);
            return Result.success(strategies);
        } catch (Exception e) {
            log.error("获取策略列表失败: {}", e.getMessage(), e);
            return Result.error("获取策略列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取策略详情
     * @param strategyId 策略ID
     * @return 策略详情
     */
    @GetMapping("/{strategyId}")
    public Result<Strategy> getStrategyDetail(@PathVariable Long strategyId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Strategy strategy = strategyService.getStrategyById(strategyId);
            
            if (strategy == null) {
                return Result.error("策略不存在");
            }
            
            if (!strategy.getUserId().equals(userId)) {
                return Result.error("无权查看此策略");
            }
            
            return Result.success(strategy);
        } catch (Exception e) {
            log.error("获取策略详情失败: {}", e.getMessage(), e);
            return Result.error("获取策略详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动执行策略
     * @param strategyId 策略ID
     * @param stockPool 股票池
     * @return 生成的信号列表
     */
    @PostMapping("/{strategyId}/execute")
    public Result<List<SignalEntity>> executeStrategy(@PathVariable Long strategyId,
                                                       @RequestBody Map<String, String> stockPool) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Strategy strategy = strategyService.getStrategyById(strategyId);
            
            if (strategy == null) {
                return Result.error("策略不存在");
            }
            
            if (!strategy.getUserId().equals(userId)) {
                return Result.error("无权执行此策略");
            }
            
            List<SignalEntity> signals = strategyService.executeStrategyManually(strategyId, stockPool);
            return Result.success(signals);
        } catch (Exception e) {
            log.error("执行策略失败: {}", e.getMessage(), e);
            return Result.error("执行策略失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取策略统计数据
     * @param strategyId 策略ID
     * @return 统计数据
     */
    @GetMapping("/{strategyId}/statistics")
    public Result<Map<String, Object>> getStrategyStatistics(@PathVariable Long strategyId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Strategy strategy = strategyService.getStrategyById(strategyId);
            
            if (strategy == null) {
                return Result.error("策略不存在");
            }
            
            if (!strategy.getUserId().equals(userId)) {
                return Result.error("无权查看此策略");
            }
            
            Map<String, Object> stats = strategyService.getStrategyStatistics(strategyId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取策略统计失败: {}", e.getMessage(), e);
            return Result.error("获取策略统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取策略的信号历史
     * @param strategyId 策略ID
     * @param limit 限制数量
     * @return 信号列表
     */
    @GetMapping("/{strategyId}/signals")
    public Result<List<SignalEntity>> getStrategySignals(@PathVariable Long strategyId,
                                                          @RequestParam(defaultValue = "50") int limit) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Strategy strategy = strategyService.getStrategyById(strategyId);
            
            if (strategy == null) {
                return Result.error("策略不存在");
            }
            
            if (!strategy.getUserId().equals(userId)) {
                return Result.error("无权查看此策略");
            }
            
            List<SignalEntity> signals = strategyService.getStrategySignals(strategyId, limit);
            return Result.success(signals);
        } catch (Exception e) {
            log.error("获取策略信号失败: {}", e.getMessage(), e);
            return Result.error("获取策略信号失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的待执行信号
     * @return 待执行信号列表
     */
    @GetMapping("/signals/pending")
    public Result<List<SignalEntity>> getPendingSignals() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            List<SignalEntity> signals = strategyService.getPendingSignals(userId);
            return Result.success(signals);
        } catch (Exception e) {
            log.error("获取待执行信号失败: {}", e.getMessage(), e);
            return Result.error("获取待执行信号失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的最近信号
     * @param limit 限制数量
     * @return 信号列表
     */
    @GetMapping("/signals/recent")
    public Result<List<SignalEntity>> getRecentSignals(@RequestParam(defaultValue = "50") int limit) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            List<SignalEntity> signals = strategyService.getRecentSignals(userId, limit);
            return Result.success(signals);
        } catch (Exception e) {
            log.error("获取最近信号失败: {}", e.getMessage(), e);
            return Result.error("获取最近信号失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取策略类型的默认参数
     * @param strategyType 策略类型
     * @return 参数模板
     */
    @GetMapping("/parameters/default")
    public Result<Map<String, Object>> getDefaultParameters(@RequestParam String strategyType) {
        try {
            Map<String, Object> params = strategyService.getDefaultParameters(strategyType);
            return Result.success(params);
        } catch (Exception e) {
            log.error("获取默认参数失败: {}", e.getMessage(), e);
            return Result.error("获取默认参数失败: " + e.getMessage());
        }
    }
}
