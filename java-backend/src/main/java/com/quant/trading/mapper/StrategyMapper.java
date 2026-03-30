package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.Strategy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

/**
 * 策略配置Mapper接口
 * 提供策略数据的数据库操作方法
 */
@Mapper
public interface StrategyMapper extends BaseMapper<Strategy> {
    
    /**
     * 根据用户ID查询所有策略
     * @param userId 用户ID
     * @return 策略列表
     */
    @Select("SELECT * FROM strategies WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Strategy> findByUserId(Long userId);
    
    /**
     * 根据用户ID和状态查询策略
     * @param userId 用户ID
     * @param status 策略状态
     * @return 策略列表
     */
    @Select("SELECT * FROM strategies WHERE user_id = #{userId} AND status = #{status} ORDER BY created_at DESC")
    List<Strategy> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
    
    /**
     * 查询所有活跃的策略
     * @return 活跃策略列表
     */
    @Select("SELECT * FROM strategies WHERE status = 'active' ORDER BY created_at DESC")
    List<Strategy> findActiveStrategies();
    
    /**
     * 更新策略统计数据
     * @param strategyId 策略ID
     * @param signalAdded 信号增量
     * @param pnl 盈亏金额
     */
    @Update("UPDATE strategies SET total_signals = total_signals + #{signalAdded}, " +
            "total_pnl = total_pnl + #{pnl}, last_executed_at = NOW(), updated_at = NOW() " +
            "WHERE id = #{strategyId}")
    int updateStatistics(@Param("strategyId") Long strategyId, 
                         @Param("signalAdded") int signalAdded, 
                         @Param("pnl") java.math.BigDecimal pnl);
    
    /**
     * 增加已执行信号计数
     * @param strategyId 策略ID
     */
    @Update("UPDATE strategies SET executed_signals = executed_signals + 1, updated_at = NOW() WHERE id = #{strategyId}")
    int incrementExecutedSignals(Long strategyId);
    
    /**
     * 增加盈利计数
     * @param strategyId 策略ID
     */
    @Update("UPDATE strategies SET win_count = win_count + 1, updated_at = NOW() WHERE id = #{strategyId}")
    int incrementWinCount(Long strategyId);
    
    /**
     * 增加亏损计数
     * @param strategyId 策略ID
     */
    @Update("UPDATE strategies SET loss_count = loss_count + 1, updated_at = NOW() WHERE id = #{strategyId}")
    int incrementLossCount(Long strategyId);
}
