package com.quant.trading;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.quant.trading.mapper")
public class QuantTradingApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuantTradingApplication.class, args);
    }
}
