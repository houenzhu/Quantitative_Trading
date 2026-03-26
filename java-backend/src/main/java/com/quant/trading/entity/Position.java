package com.quant.trading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("positions")
public class Position {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("stock_code")
    private String stockCode;
    
    @TableField("stock_name")
    private String stockName;
    
    private Integer quantity;
    
    @TableField("available_quantity")
    private Integer availableQuantity;
    
    @TableField("avg_cost")
    private BigDecimal avgCost;
    
    @TableField("current_price")
    private BigDecimal currentPrice;
    
    @TableField("market_value")
    private BigDecimal marketValue;
    
    @TableField("unrealized_pnl")
    private BigDecimal unrealizedPnl;
    
    @TableField("unrealized_pnl_pct")
    private BigDecimal unrealizedPnlPct;
    
    @TableField(value = "opened_at", fill = FieldFill.INSERT)
    private LocalDateTime openedAt;
    
    @TableField(value = "updated_at", fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField("closed_at")
    private LocalDateTime closedAt;
    
    @TableField("is_active")
    private Boolean isActive;
}
