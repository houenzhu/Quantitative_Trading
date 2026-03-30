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
public class VWAPStrategy implements Strategy {
    
    private static final Logger logger = LoggerFactory.getLogger(VWAPStrategy.class);
    
    private static final String NAME = "VWAP";
    
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
        BigDecimal deviationThreshold = new BigDecimal("0.003");
        
        if (parametersJson != null && !parametersJson.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> params = objectMapper.readValue(parametersJson, java.util.Map.class);
                
                if (params.containsKey("deviationThreshold")) {
                    deviationThreshold = new BigDecimal(params.get("deviationThreshold").toString());
                }
            } catch (Exception e) {
                logger.warn("解析VWAP策略参数失败，使用默认值: {}", e.getMessage());
            }
        }
        
        if (history == null || history.isEmpty() || currentTick == null) {
            return null;
        }
        
        BigDecimal vwap = calculateVWAP(history);
        
        if (vwap == null || vwap.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        BigDecimal currentPrice = currentTick.getPrice();
        if (currentPrice == null) {
            return null;
        }
        
        BigDecimal deviation = currentPrice.subtract(vwap).divide(vwap, 4, RoundingMode.HALF_UP);
        
        Signal signal = new Signal();
        signal.setStockCode(stockCode);
        signal.setStockName(stockName);
        signal.setStrategyName(NAME);
        signal.setTimestamp(LocalDateTime.now());
        
        if (deviation.compareTo(deviationThreshold.negate()) < 0) {
            signal.setAction("buy");
            signal.setStrength(deviation.abs().multiply(new BigDecimal("100")));
            signal.setReason(String.format("价格低于VWAP %.2f%%, VWAP=%.2f, 当前=%.2f (阈值: %.2f%%)", 
                    deviation.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                    vwap.setScale(2, RoundingMode.HALF_UP),
                    currentPrice.setScale(2, RoundingMode.HALF_UP),
                    deviationThreshold.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
            logger.info("VWAP策略触发买入: {} - {}", stockCode, signal.getReason());
        } else if (deviation.compareTo(deviationThreshold) > 0) {
            signal.setAction("sell");
            signal.setStrength(deviation.multiply(new BigDecimal("100")));
            signal.setReason(String.format("价格高于VWAP %.2f%%, VWAP=%.2f, 当前=%.2f (阈值: %.2f%%)", 
                    deviation.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                    vwap.setScale(2, RoundingMode.HALF_UP),
                    currentPrice.setScale(2, RoundingMode.HALF_UP),
                    deviationThreshold.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
            logger.info("VWAP策略触发卖出: {} - {}", stockCode, signal.getReason());
        } else {
            return null;
        }
        
        return signal;
    }
    
    private BigDecimal calculateVWAP(List<TickData> history) {
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        
        for (TickData tick : history) {
            if (tick.getPrice() != null && tick.getVolume() != null && tick.getVolume().compareTo(BigDecimal.ZERO) > 0) {
                totalValue = totalValue.add(tick.getPrice().multiply(tick.getVolume()));
                totalVolume = totalVolume.add(tick.getVolume());
            }
        }
        
        if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        return totalValue.divide(totalVolume, 4, RoundingMode.HALF_UP);
    }
    
    @Override
    public void reset() {
    }
}
