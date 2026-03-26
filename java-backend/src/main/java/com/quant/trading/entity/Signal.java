package com.quant.trading.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Signal {
    private String stockCode;
    private String stockName;
    private String action;
    private BigDecimal strength;
    private String reason;
    private String strategyName;
    private LocalDateTime timestamp;
}
