package com.quant.trading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略配置实体类
 * 用于存储用户的交易策略配置信息
 */
@Data
@TableName("strategies")
public class Strategy {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    private String name;
    
    private String description;
    
    @TableField("strategy_type")
    private String strategyType;
    
    private String status;
    
    @TableField("parameters")
    private String parameters;
    
    @TableField("max_position_pct")
    private BigDecimal maxPositionPct;
    
    @TableField("stop_loss_pct")
    private BigDecimal stopLossPct;
    
    @TableField("take_profit_pct")
    private BigDecimal takeProfitPct;
    
    @TableField("max_trades_per_day")
    private Integer maxTradesPerDay;
    
    @TableField("trade_size")
    private Integer tradeSize;
    
    @TableField("allow_short")
    private Boolean allowShort;
    
    @TableField("auto_execute")
    private Boolean autoExecute;
    
    @TableField("total_signals")
    private Integer totalSignals;
    
    @TableField("executed_signals")
    private Integer executedSignals;
    
    @TableField("win_count")
    private Integer winCount;
    
    @TableField("loss_count")
    private Integer lossCount;
    
    @TableField("total_pnl")
    private BigDecimal totalPnl;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField("last_executed_at")
    private LocalDateTime lastExecutedAt;
}
