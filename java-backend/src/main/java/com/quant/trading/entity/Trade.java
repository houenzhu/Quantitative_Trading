package com.quant.trading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("trades")
public class Trade {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("trade_id")
    private String tradeId;
    
    @TableField("order_id")
    private String orderId;
    
    @TableField("stock_code")
    private String stockCode;
    
    @TableField("stock_name")
    private String stockName;
    
    private String side;
    
    private Integer quantity;
    
    private BigDecimal price;
    
    private BigDecimal amount;
    
    private BigDecimal commission;
    
    @TableField("stamp_duty")
    private BigDecimal stampDuty;
    
    @TableField("transfer_fee")
    private BigDecimal transferFee;
    
    @TableField("slippage_cost")
    private BigDecimal slippageCost;
    
    @TableField("realized_pnl")
    private BigDecimal realizedPnl;
    
    @TableField("traded_at")
    private LocalDateTime tradedAt;
    
    @TableField("strategy_reason")
    private String strategyReason;
}
