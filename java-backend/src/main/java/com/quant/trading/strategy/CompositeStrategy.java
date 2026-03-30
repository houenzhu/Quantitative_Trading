package com.quant.trading.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.trading.entity.Signal;
import com.quant.trading.entity.TickData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CompositeStrategy implements Strategy {
    
    private static final Logger logger = LoggerFactory.getLogger(CompositeStrategy.class);
    
    private static final String NAME = "Composite";
    
    @Autowired
    private MomentumStrategy momentumStrategy;
    
    @Autowired
    private VWAPStrategy vwapStrategy;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public Signal analyze(String stockCode, String stockName, TickData currentTick, List<TickData> history) {
        return analyze(stockCode, stockName, currentTick, history, null);
    }
    
    public Signal analyze(String stockCode, String stockName, TickData currentTick, List<TickData> history, String parametersJson) {
        Signal momentumSignal = momentumStrategy.analyze(stockCode, stockName, currentTick, history, parametersJson);
        Signal vwapSignal = vwapStrategy.analyze(stockCode, stockName, currentTick, history, parametersJson);
        
        if (momentumSignal == null && vwapSignal == null) {
            return null;
        }
        
        if (momentumSignal != null && vwapSignal != null) {
            if (momentumSignal.getAction().equals(vwapSignal.getAction())) {
                Signal combined = new Signal();
                combined.setStockCode(stockCode);
                combined.setStockName(stockName);
                combined.setAction(momentumSignal.getAction());
                combined.setStrength(momentumSignal.getStrength().add(vwapSignal.getStrength()).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP));
                combined.setReason("复合信号确认: " + momentumSignal.getAction() + " - " + momentumSignal.getReason() + " | " + vwapSignal.getReason());
                combined.setStrategyName(NAME);
                combined.setTimestamp(LocalDateTime.now());
                logger.info("复合策略触发: {} - {}", stockCode, combined.getReason());
                return combined;
            }
            logger.debug("复合策略信号冲突: 动量={}, VWAP={}, 跳过", momentumSignal.getAction(), vwapSignal.getAction());
            return null;
        }
        
        Signal singleSignal = momentumSignal != null ? momentumSignal : vwapSignal;
        logger.info("复合策略单一信号: {} - {} - {}", stockCode, singleSignal.getAction(), singleSignal.getReason());
        return singleSignal;
    }
    
    @Override
    public void reset() {
        momentumStrategy.reset();
        vwapStrategy.reset();
    }
}
