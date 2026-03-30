package com.quant.trading.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.trading.entity.Signal;
import com.quant.trading.entity.TickData;

@Component
public class MomentumStrategy implements Strategy {
    
    private static final Logger logger = LoggerFactory.getLogger(MomentumStrategy.class);
    
    private static final String NAME = "Momentum";
    
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
        int lookbackPeriod = 5;
        BigDecimal momentumThreshold = new BigDecimal("0.005");
        
        if (parametersJson != null && !parametersJson.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = objectMapper.readValue(parametersJson, Map.class);
                
                if (params.containsKey("lookbackPeriod")) {
                    lookbackPeriod = ((Number) params.get("lookbackPeriod")).intValue();
                }
                if (params.containsKey("momentumThreshold")) {
                    momentumThreshold = new BigDecimal(params.get("momentumThreshold").toString());
                }
            } catch (Exception e) {
                logger.warn("解析动量策略参数失败，使用默认值: {}", e.getMessage());
            }
        }
        
        if (history == null || history.size() < lookbackPeriod) {
            logger.debug("历史数据不足: {} 需要{}个, 当前{}个", stockCode, lookbackPeriod, history == null ? 0 : history.size());
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
            signal.setReason(String.format("动量突破买入信号: %.2f%% (阈值: %.2f%%)", 
                    momentum.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                    momentumThreshold.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
            logger.info("动量策略触发买入: {} - {}", stockCode, signal.getReason());
        } else if (momentum.compareTo(momentumThreshold.negate()) < 0) {
            signal.setAction("sell");
            signal.setStrength(momentum.abs().multiply(new BigDecimal("100")));
            signal.setReason(String.format("动量跌破卖出信号: %.2f%% (阈值: %.2f%%)", 
                    momentum.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                    momentumThreshold.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
            logger.info("动量策略触发卖出: {} - {}", stockCode, signal.getReason());
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
}
