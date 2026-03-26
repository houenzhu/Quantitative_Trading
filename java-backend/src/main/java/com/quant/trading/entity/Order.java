package com.quant.trading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("order_id")
    private String orderId;
    
    @TableField("stock_code")
    private String stockCode;
    
    @TableField("stock_name")
    private String stockName;
    
    private String side;
    
    @TableField("order_type")
    private String orderType;
    
    private BigDecimal price;
    
    private Integer quantity;
    
    @TableField("filled_quantity")
    private Integer filledQuantity;
    
    @TableField("avg_fill_price")
    private BigDecimal avgFillPrice;
    
    private String status;
    
    private String reason;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField("submitted_at")
    private LocalDateTime submittedAt;
    
    @TableField("filled_at")
    private LocalDateTime filledAt;
    
    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;
    
    private BigDecimal commission;
    
    private BigDecimal slippage;
    
    @TableField("error_message")
    private String errorMessage;
}
