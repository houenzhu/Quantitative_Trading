package com.quant.trading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("equity_snapshots")
public class EquitySnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("snapshot_date")
    private String snapshotDate;
    
    private LocalDateTime timestamp;
    
    @TableField("total_equity")
    private BigDecimal totalEquity;
    
    private BigDecimal cash;
    
    @TableField("positions_value")
    private BigDecimal positionsValue;
    
    @TableField("daily_pnl")
    private BigDecimal dailyPnl;
    
    @TableField("daily_pnl_pct")
    private BigDecimal dailyPnlPct;
    
    @TableField("cumulative_pnl")
    private BigDecimal cumulativePnl;
    
    @TableField("cumulative_pnl_pct")
    private BigDecimal cumulativePnlPct;
    
    private BigDecimal drawdown;
    
    @TableField("drawdown_pct")
    private BigDecimal drawdownPct;
    
    @TableField("peak_equity")
    private BigDecimal peakEquity;
    
    @TableField("trade_count_today")
    private Integer tradeCountToday;
    
    @TableField("position_count")
    private Integer positionCount;
}
