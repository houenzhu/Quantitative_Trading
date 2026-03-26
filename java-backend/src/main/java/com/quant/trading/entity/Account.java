package com.quant.trading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("accounts")
public class Account {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private LocalDateTime timestamp;
    
    @TableField("initial_capital")
    private BigDecimal initialCapital;
    
    @TableField("current_capital")
    private BigDecimal currentCapital;
    
    @TableField("total_equity")
    private BigDecimal totalEquity;
    
    private BigDecimal cash;
    
    @TableField("positions_value")
    private BigDecimal positionsValue;
    
    @TableField("total_pnl")
    private BigDecimal totalPnl;
    
    @TableField("realized_pnl")
    private BigDecimal realizedPnl;
    
    @TableField("unrealized_pnl")
    private BigDecimal unrealizedPnl;
    
    @TableField("daily_pnl")
    private BigDecimal dailyPnl;
    
    @TableField("daily_loss")
    private BigDecimal dailyLoss;
    
    @TableField("trade_count")
    private Integer tradeCount;
    
    @TableField("win_count")
    private Integer winCount;
    
    @TableField("loss_count")
    private Integer lossCount;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
