package com.quant.trading.strategy;

import com.quant.trading.entity.Signal;
import com.quant.trading.entity.TickData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CompositeStrategy implements Strategy {
    
    private static final Logger logger = LoggerFactory.getLogger(CompositeStrategy.class);
    
    private static final String NAME = "Composite";
    
    private final MomentumStrategy momentumStrategy;
    private final VWAPStrategy vwapStrategy;
    
    public CompositeStrategy(MomentumStrategy momentumStrategy, VWAPStrategy vwapStrategy) {
        this.momentumStrategy = momentumStrategy;
        this.vwapStrategy = vwapStrategy;
    }
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public Signal analyze(String stockCode, String stockName, TickData currentTick, List<TickData> history) {
        Signal momentumSignal = momentumStrategy.analyze(stockCode, stockName, currentTick, history);
        Signal vwapSignal = vwapStrategy.analyze(stockCode, stockName, currentTick, history);
        
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
                return combined;
            }
            return null;
        }
        
        return momentumSignal != null ? momentumSignal : vwapSignal;
    }
    
    @Override
    public void reset() {
        momentumStrategy.reset();
        vwapStrategy.reset();
    }
}
