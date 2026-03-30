package com.quant.trading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易信号实体类
 * 用于存储策略生成的买卖信号
 */
@Data
@TableName("signals")
public class SignalEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("strategy_id")
    private Long strategyId;
    
    @TableField("stock_code")
    private String stockCode;
    
    @TableField("stock_name")
    private String stockName;
    
    @TableField("signal_type")
    private String signalType;
    
    @TableField("signal_strength")
    private BigDecimal signalStrength;
    
    private BigDecimal price;
    
    private String reason;
    
    private String indicators;
    
    private String status;
    
    @TableField("executed_at")
    private LocalDateTime executedAt;
    
    @TableField("executed_price")
    private BigDecimal executedPrice;
    
    @TableField("order_id")
    private String orderId;
    
    private BigDecimal pnl;
    
    @TableField("pnl_pct")
    private BigDecimal pnlPct;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField("expired_at")
    private LocalDateTime expiredAt;
}
