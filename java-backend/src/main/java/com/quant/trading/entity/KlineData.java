package com.quant.trading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K线数据实体类
 * 用于存储股票的历史K线数据，支持多种时间周期
 */
@Data
@TableName("kline_data")
public class KlineData {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("stock_code")
    private String stockCode;
    
    private String period;
    
    @TableField("open_price")
    private BigDecimal openPrice;
    
    @TableField("high_price")
    private BigDecimal highPrice;
    
    @TableField("low_price")
    private BigDecimal lowPrice;
    
    @TableField("close_price")
    private BigDecimal closePrice;
    
    private Long volume;
    
    private BigDecimal amount;
    
    @TableField("trade_time")
    private LocalDateTime tradeTime;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
