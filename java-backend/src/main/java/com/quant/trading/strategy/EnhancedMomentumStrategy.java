package com.quant.trading.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class EnhancedMomentumStrategy implements Strategy {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedMomentumStrategy.class);
    
    private static final String NAME = "EnhancedMomentum";
    
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
        int lookbackPeriod = 15;
        BigDecimal momentumThreshold = new BigDecimal("0.004");
        int shortMaPeriod = 5;
        int longMaPeriod = 20;
        BigDecimal volumeMultiplier = new BigDecimal("1.5");
        BigDecimal maxDrawdownThreshold = new BigDecimal("0.03");
        boolean requireTrendConfirm = true;
        boolean requireVolumeConfirm = true;
        
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
                if (params.containsKey("shortMaPeriod")) {
                    shortMaPeriod = ((Number) params.get("shortMaPeriod")).intValue();
                }
                if (params.containsKey("longMaPeriod")) {
                    longMaPeriod = ((Number) params.get("longMaPeriod")).intValue();
                }
                if (params.containsKey("volumeMultiplier")) {
                    volumeMultiplier = new BigDecimal(params.get("volumeMultiplier").toString());
                }
                if (params.containsKey("maxDrawdownThreshold")) {
                    maxDrawdownThreshold = new BigDecimal(params.get("maxDrawdownThreshold").toString());
                }
                if (params.containsKey("requireTrendConfirm")) {
                    requireTrendConfirm = Boolean.parseBoolean(params.get("requireTrendConfirm").toString());
                }
                if (params.containsKey("requireVolumeConfirm")) {
                    requireVolumeConfirm = Boolean.parseBoolean(params.get("requireVolumeConfirm").toString());
                }
            } catch (Exception e) {
                logger.warn("解析增强动量策略参数失败，使用默认值: {}", e.getMessage());
            }
        }
        
        if (history == null || history.size() < longMaPeriod) {
            logger.debug("历史数据不足: {} 需要{}个, 当前{}个", stockCode, longMaPeriod, history == null ? 0 : history.size());
            return null;
        }
        
        BigDecimal currentPrice = currentTick.getPrice();
        
        BigDecimal momentum = calculateTimeSeriesMomentum(history, currentPrice, lookbackPeriod);
        if (momentum == null) {
            return null;
        }
        
        BigDecimal volatility = calculateVolatility(history, lookbackPeriod);
        
        BigDecimal adjustedThreshold = momentumThreshold;
        if (volatility != null && volatility.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal volatilityAdjustment = volatility.multiply(new BigDecimal("0.5"));
            adjustedThreshold = momentumThreshold.add(volatilityAdjustment);
        }
        
        boolean trendConfirm = true;
        if (requireTrendConfirm) {
            trendConfirm = checkTrendConfirmation(history, currentPrice, shortMaPeriod, longMaPeriod);
        }
        
        boolean volumeConfirm = true;
        if (requireVolumeConfirm && currentTick.getVolume() != null) {
            volumeConfirm = checkVolumeConfirmation(history, currentTick.getVolume().longValue(), volumeMultiplier);
        }
        
        BigDecimal maxDrawdown = calculateMaxDrawdown(history);
        boolean drawdownSafe = maxDrawdown == null || maxDrawdown.abs().compareTo(maxDrawdownThreshold) <= 0;
        
        BigDecimal trendStrength = calculateTrendStrength(history, lookbackPeriod);
        
        logger.info("增强动量分析: {} 价格={} 动量={}% 调整阈值={}% 波动率={}% 趋势确认={} 成交量确认={} 最大回撤={}% 趋势强度={}", 
                stockCode, currentPrice, 
                momentum.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                adjustedThreshold.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                volatility != null ? volatility.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) : "N/A",
                trendConfirm, volumeConfirm,
                maxDrawdown != null ? maxDrawdown.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) : "N/A",
                trendStrength != null ? trendStrength.setScale(2, RoundingMode.HALF_UP) : "N/A");
        
        Signal signal = new Signal();
        signal.setStockCode(stockCode);
        signal.setStockName(stockName);
        signal.setStrategyName(NAME);
        signal.setTimestamp(LocalDateTime.now());
        
        StringBuilder reasonBuilder = new StringBuilder();
        List<String> confirmations = new ArrayList<>();
        
        if (momentum.compareTo(adjustedThreshold) > 0 && trendConfirm && volumeConfirm && drawdownSafe) {
            signal.setAction("buy");
            signal.setStrength(momentum.multiply(new BigDecimal("100")));
            
            reasonBuilder.append(String.format("增强动量买入: 动量%.2f%%>", 
                    momentum.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
            reasonBuilder.append(String.format("阈值%.2f%%", 
                    adjustedThreshold.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
            
            if (trendConfirm) confirmations.add("趋势确认");
            if (volumeConfirm) confirmations.add("放量");
            if (drawdownSafe) confirmations.add("回撤安全");
            if (trendStrength != null && trendStrength.compareTo(new BigDecimal("60")) > 0) {
                confirmations.add("强趋势");
            }
            
            if (!confirmations.isEmpty()) {
                reasonBuilder.append(" [").append(String.join("+", confirmations)).append("]");
            }
            
            signal.setReason(reasonBuilder.toString());
            logger.info("增强动量策略触发买入: {} - {}", stockCode, signal.getReason());
            
        } else if (momentum.compareTo(adjustedThreshold.negate()) < 0) {
            signal.setAction("sell");
            signal.setStrength(momentum.abs().multiply(new BigDecimal("100")));
            
            reasonBuilder.append(String.format("增强动量卖出: 动量%.2f%%<", 
                    momentum.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
            reasonBuilder.append(String.format("-阈值%.2f%%", 
                    adjustedThreshold.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)));
            
            if (!trendConfirm) reasonBuilder.append(" [趋势破坏]");
            if (maxDrawdown != null && maxDrawdown.abs().compareTo(maxDrawdownThreshold) > 0) {
                reasonBuilder.append(" [触发止损]");
            }
            
            signal.setReason(reasonBuilder.toString());
            logger.info("增强动量策略触发卖出: {} - {}", stockCode, signal.getReason());
            
        } else {
            if (momentum.compareTo(adjustedThreshold) > 0) {
                logger.debug("动量达标但未满足确认条件: {} 趋势确认={} 成交量确认={} 回撤安全={}", 
                        stockCode, trendConfirm, volumeConfirm, drawdownSafe);
            }
            return null;
        }
        
        return signal;
    }
    
    private BigDecimal calculateTimeSeriesMomentum(List<TickData> history, BigDecimal currentPrice, int period) {
        if (history.size() < period || currentPrice == null) {
            return null;
        }
        
        List<TickData> recentHistory = history.subList(history.size() - period, history.size());
        TickData oldest = recentHistory.get(0);
        
        if (oldest.getPrice() == null || oldest.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        BigDecimal priceChange = currentPrice.subtract(oldest.getPrice());
        return priceChange.divide(oldest.getPrice(), 4, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateVolatility(List<TickData> history, int period) {
        if (history.size() < period) {
            return null;
        }
        
        List<TickData> recentHistory = history.subList(history.size() - period, history.size());
        
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal sumSq = BigDecimal.ZERO;
        int count = 0;
        
        for (int i = 1; i < recentHistory.size(); i++) {
            TickData prev = recentHistory.get(i - 1);
            TickData curr = recentHistory.get(i);
            
            if (prev.getPrice() != null && curr.getPrice() != null && 
                prev.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                
                BigDecimal ret = curr.getPrice().subtract(prev.getPrice())
                        .divide(prev.getPrice(), 6, RoundingMode.HALF_UP);
                sum = sum.add(ret);
                sumSq = sumSq.add(ret.multiply(ret));
                count++;
            }
        }
        
        if (count < 2) {
            return null;
        }
        
        BigDecimal mean = sum.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
        BigDecimal variance = sumSq.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP)
                .subtract(mean.multiply(mean));
        
        if (variance.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
    
    private boolean checkTrendConfirmation(List<TickData> history, BigDecimal currentPrice, int shortPeriod, int longPeriod) {
        if (history.size() < longPeriod) {
            return false;
        }
        
        BigDecimal shortMa = calculateMA(history, shortPeriod);
        BigDecimal longMa = calculateMA(history, longPeriod);
        
        if (shortMa == null || longMa == null) {
            return false;
        }
        
        boolean maBullish = shortMa.compareTo(longMa) > 0;
        boolean priceAboveShortMa = currentPrice.compareTo(shortMa) > 0;
        
        return maBullish && priceAboveShortMa;
    }
    
    private BigDecimal calculateMA(List<TickData> history, int period) {
        if (history.size() < period) {
            return null;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        
        for (int i = history.size() - period; i < history.size(); i++) {
            TickData tick = history.get(i);
            if (tick.getPrice() != null) {
                sum = sum.add(tick.getPrice());
                count++;
            }
        }
        
        if (count == 0) {
            return null;
        }
        
        return sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }
    
    private boolean checkVolumeConfirmation(List<TickData> history, Long currentVolume, BigDecimal multiplier) {
        if (currentVolume == null || history.size() < 10) {
            return true;
        }
        
        long sumVolume = 0;
        int count = 0;
        
        int startIdx = Math.max(0, history.size() - 20);
        for (int i = startIdx; i < history.size(); i++) {
            Long vol = history.get(i).getVolume().longValue();
            if (vol != null) {
                sumVolume += vol;
                count++;
            }
        }
        
        if (count == 0) {
            return true;
        }
        
        double avgVolume = (double) sumVolume / count;
        double threshold = avgVolume * multiplier.doubleValue();
        
        return currentVolume >= threshold;
    }
    
    private BigDecimal calculateMaxDrawdown(List<TickData> history) {
        if (history.isEmpty()) {
            return null;
        }
        
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        
        for (TickData tick : history) {
            if (tick.getPrice() == null) continue;
            
            if (tick.getPrice().compareTo(peak) > 0) {
                peak = tick.getPrice();
            }
            
            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal drawdown = tick.getPrice().subtract(peak).divide(peak, 4, RoundingMode.HALF_UP);
                if (drawdown.compareTo(maxDrawdown) < 0) {
                    maxDrawdown = drawdown;
                }
            }
        }
        
        return maxDrawdown;
    }
    
    private BigDecimal calculateTrendStrength(List<TickData> history, int period) {
        if (history.size() < period) {
            return null;
        }
        
        List<TickData> recentHistory = history.subList(history.size() - period, history.size());
        
        int upCount = 0;
        int downCount = 0;
        int totalMoves = 0;
        
        for (int i = 1; i < recentHistory.size(); i++) {
            TickData prev = recentHistory.get(i - 1);
            TickData curr = recentHistory.get(i);
            
            if (prev.getPrice() != null && curr.getPrice() != null) {
                if (curr.getPrice().compareTo(prev.getPrice()) > 0) {
                    upCount++;
                } else if (curr.getPrice().compareTo(prev.getPrice()) < 0) {
                    downCount++;
                }
                totalMoves++;
            }
        }
        
        if (totalMoves == 0) {
            return null;
        }
        
        return BigDecimal.valueOf((double) upCount / totalMoves * 100);
    }
    
    @Override
    public void reset() {
    }
}
