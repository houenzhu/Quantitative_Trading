package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.SignalEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易信号Mapper接口
 * 提供信号数据的数据库操作方法
 */
@Mapper
public interface SignalMapper extends BaseMapper<SignalEntity> {
    
    /**
     * 根据用户ID查询信号列表
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 信号列表
     */
    @Select("SELECT * FROM signals WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<SignalEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 根据策略ID查询信号列表
     * @param strategyId 策略ID
     * @param limit 限制数量
     * @return 信号列表
     */
    @Select("SELECT * FROM signals WHERE strategy_id = #{strategyId} ORDER BY created_at DESC LIMIT #{limit}")
    List<SignalEntity> findByStrategyId(@Param("strategyId") Long strategyId, @Param("limit") int limit);
    
    /**
     * 查询待执行的信号
     * @param userId 用户ID
     * @return 待执行信号列表
     */
    @Select("SELECT * FROM signals WHERE user_id = #{userId} AND status = 'pending' AND (expired_at IS NULL OR expired_at > NOW()) ORDER BY created_at ASC")
    List<SignalEntity> findPendingSignals(Long userId);
    
    /**
     * 根据股票代码和状态查询最近的信号
     * @param userId 用户ID
     * @param stockCode 股票代码
     * @param signalType 信号类型
     * @param status 状态
     * @return 最近的信号
     */
    @Select("SELECT * FROM signals WHERE user_id = #{userId} AND stock_code = #{stockCode} " +
            "AND signal_type = #{signalType} AND status = #{status} " +
            "ORDER BY created_at DESC LIMIT 1")
    SignalEntity findLatestSignal(@Param("userId") Long userId, 
                                   @Param("stockCode") String stockCode,
                                   @Param("signalType") String signalType,
                                   @Param("status") String status);
    
    /**
     * 更新信号状态为已执行
     * @param signalId 信号ID
     * @param executedPrice 执行价格
     * @param orderId 订单ID
     */
    @Update("UPDATE signals SET status = 'executed', executed_at = NOW(), " +
            "executed_price = #{executedPrice}, order_id = #{orderId} WHERE id = #{signalId}")
    int markAsExecuted(@Param("signalId") Long signalId, 
                       @Param("executedPrice") java.math.BigDecimal executedPrice,
                       @Param("orderId") String orderId);
    
    /**
     * 更新信号盈亏结果
     * @param signalId 信号ID
     * @param pnl 盈亏金额
     * @param pnlPct 盈亏比例
     */
    @Update("UPDATE signals SET pnl = #{pnl}, pnl_pct = #{pnlPct} WHERE id = #{signalId}")
    int updatePnl(@Param("signalId") Long signalId, 
                  @Param("pnl") java.math.BigDecimal pnl,
                  @Param("pnlPct") java.math.BigDecimal pnlPct);
    
    /**
     * 标记过期的信号
     * @param beforeTime 过期时间点
     * @return 更新的记录数
     */
    @Update("UPDATE signals SET status = 'expired' WHERE status = 'pending' AND expired_at IS NOT NULL AND expired_at < #{beforeTime}")
    int markExpiredSignals(LocalDateTime beforeTime);
}
