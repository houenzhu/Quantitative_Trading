package com.quant.trading.strategy;

import com.quant.trading.entity.Signal;
import com.quant.trading.entity.TickData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class MomentumStrategy implements Strategy {
    
    private static final Logger logger = LoggerFactory.getLogger(MomentumStrategy.class);
    
    private static final String NAME = "Momentum";
    
    private int lookbackPeriod = 5;
    private BigDecimal momentumThreshold = new BigDecimal("0.02");
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public Signal analyze(String stockCode, String stockName, TickData currentTick, List<TickData> history) {
        if (history == null || history.size() < lookbackPeriod) {
            return null;
        }
        
        List<TickData> recentHistory = history.size() > lookbackPeriod 
                ? history.subList(history.size() - lookbackPeriod, history.size())
                : history;
        
        BigDecimal momentum = calculateMomentum(recentHistory, currentTick);
        
        if (momentum == null) {
            return null;
        }
        
        Signal signal = new Signal();
        signal.setStockCode(stockCode);
        signal.setStockName(stockName);
        signal.setStrategyName(NAME);
        signal.setTimestamp(LocalDateTime.now());
        
        if (momentum.compareTo(momentumThreshold) > 0) {
            signal.setAction("buy");
            signal.setStrength(momentum.multiply(new BigDecimal("100")));
            signal.setReason("动量突破买入信号: " + momentum.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%");
        } else if (momentum.compareTo(momentumThreshold.negate()) < 0) {
            signal.setAction("sell");
            signal.setStrength(momentum.abs().multiply(new BigDecimal("100")));
            signal.setReason("动量跌破卖出信号: " + momentum.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%");
        } else {
            return null;
        }
        
        return signal;
    }
    
    private BigDecimal calculateMomentum(List<TickData> history, TickData current) {
        if (history.isEmpty() || current == null || current.getPrice() == null) {
            return null;
        }
        
        TickData oldest = history.get(0);
        if (oldest.getPrice() == null || oldest.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        BigDecimal priceChange = current.getPrice().subtract(oldest.getPrice());
        return priceChange.divide(oldest.getPrice(), 4, RoundingMode.HALF_UP);
    }
    
    @Override
    public void reset() {
    }
    
    public void setLookbackPeriod(int lookbackPeriod) {
        this.lookbackPeriod = lookbackPeriod;
    }
    
    public void setMomentumThreshold(BigDecimal momentumThreshold) {
        this.momentumThreshold = momentumThreshold;
    }
}
