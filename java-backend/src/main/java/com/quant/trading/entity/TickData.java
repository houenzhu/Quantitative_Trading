package com.quant.trading.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TickData {
    private String code;
    private String name;
    private BigDecimal price;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal preClose;
    private BigDecimal volume;
    private BigDecimal amount;
    private BigDecimal bid1;
    private BigDecimal ask1;
    private BigDecimal bid1Volume;
    private BigDecimal ask1Volume;
    private BigDecimal bid2;
    private BigDecimal ask2;
    private BigDecimal bid2Volume;
    private BigDecimal ask2Volume;
    private BigDecimal bid3;
    private BigDecimal ask3;
    private BigDecimal bid3Volume;
    private BigDecimal ask3Volume;
    private BigDecimal bid4;
    private BigDecimal ask4;
    private BigDecimal bid4Volume;
    private BigDecimal ask4Volume;
    private BigDecimal bid5;
    private BigDecimal ask5;
    private BigDecimal bid5Volume;
    private BigDecimal ask5Volume;
    private LocalDateTime time;
    private BigDecimal change;
    private BigDecimal changePct;
}
