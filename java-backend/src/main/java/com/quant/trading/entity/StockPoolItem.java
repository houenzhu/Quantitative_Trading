package com.quant.trading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("stock_pool")
public class StockPoolItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("stock_code")
    private String stockCode;
    
    @TableField("stock_name")
    private String stockName;
    
    @TableField(value = "added_at", fill = FieldFill.INSERT)
    private LocalDateTime addedAt;
    
    @TableField("removed_at")
    private LocalDateTime removedAt;
    
    @TableField("is_active")
    private Boolean isActive;
    
    private Integer priority;
}
