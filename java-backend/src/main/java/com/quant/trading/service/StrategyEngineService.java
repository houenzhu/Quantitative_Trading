package com.quant.trading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.trading.entity.KlineData;
import com.quant.trading.entity.SignalEntity;
import com.quant.trading.entity.Strategy;
import com.quant.trading.mapper.KlineDataMapper;
import com.quant.trading.mapper.SignalMapper;
import com.quant.trading.mapper.StrategyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 策略引擎服务
 * 核心功能：根据策略配置分析市场数据并生成交易信号
 * 
 * 支持的策略类型：
 * 1. MA_CROSSOVER - 均线交叉策略
 * 2. MACD - MACD策略
 * 3. RSI - RSI超买超卖策略
 * 4. BOLLINGER - 布林带策略
 * 5. CUSTOM - 自定义策略
 */
@Service
@Slf4j
public class StrategyEngineService {
    
    @Autowired
    private StrategyMapper strategyMapper;
    
    @Autowired
    private SignalMapper signalMapper;
    
    @Autowired
    private KlineDataMapper klineDataMapper;
    
    @Autowired
    private TechnicalIndicatorService indicatorService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 执行指定策略，分析股票池中的所有股票
     * @param strategyId 策略ID
     * @param stockPool 股票池(Map<股票代码, 股票名称>)
     * @return 生成的信号列表
     */
    public List<SignalEntity> executeStrategy(Long strategyId, Map<String, String> stockPool) {
        List<SignalEntity> signals = new ArrayList<>();
        
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            log.error("策略不存在: {}", strategyId);
            return signals;
        }
        
        if (!"active".equals(strategy.getStatus())) {
            log.info("策略未激活，跳过执行: {}", strategy.getName());
            return signals;
        }
        
        log.info("开始执行策略: {} (类型: {})", strategy.getName(), strategy.getStrategyType());
        
        for (Map.Entry<String, String> entry : stockPool.entrySet()) {
            String stockCode = entry.getKey();
            String stockName = entry.getValue();
            
            try {
                SignalEntity signal = analyzeStock(strategy, stockCode, stockName);
                if (signal != null) {
                    signals.add(signal);
                    saveSignal(signal);
                    strategyMapper.updateStatistics(strategyId, 1, BigDecimal.ZERO);
                }
            } catch (Exception e) {
                log.error("分析股票{}时发生错误: {}", stockCode, e.getMessage(), e);
            }
        }
        
        log.info("策略执行完成，生成{}个信号", signals.size());
        return signals;
    }
    
    /**
     * 分析单只股票，根据策略类型调用不同的分析方法
     * @param strategy 策略配置
     * @param stockCode 股票代码
     * @param stockName 股票名称
     * @return 生成的信号，如果没有信号则返回null
     */
    private SignalEntity analyzeStock(Strategy strategy, String stockCode, String stockName) {
        String strategyType = strategy.getStrategyType();
        
        switch (strategyType) {
            case "MA_CROSSOVER":
                return analyzeMACrossover(strategy, stockCode, stockName);
            case "MACD":
                return analyzeMACD(strategy, stockCode, stockName);
            case "RSI":
                return analyzeRSI(strategy, stockCode, stockName);
            case "BOLLINGER":
                return analyzeBollinger(strategy, stockCode, stockName);
            default:
                log.warn("未知的策略类型: {}", strategyType);
                return null;
        }
    }
    
    /**
     * 均线交叉策略分析
     * 买入信号：短期均线上穿长期均线（金叉）
     * 卖出信号：短期均线下穿长期均线（死叉）
     * 
     * @param strategy 策略配置
     * @param stockCode 股票代码
     * @param stockName 股票名称
     * @return 交易信号
     */
    private SignalEntity analyzeMACrossover(Strategy strategy, String stockCode, String stockName) {
        try {
            Map<String, Object> params = objectMapper.readValue(strategy.getParameters(), Map.class);
            int shortPeriod = ((Number) params.getOrDefault("shortPeriod", 5)).intValue();
            int longPeriod = ((Number) params.getOrDefault("longPeriod", 20)).intValue();
            String klinePeriod = (String) params.getOrDefault("klinePeriod", "1d");
            
            List<KlineData> klines = klineDataMapper.findLatestKlines(stockCode, klinePeriod, longPeriod + 10);
            if (klines == null || klines.size() < longPeriod + 2) {
                log.debug("股票{}K线数据不足，跳过分析", stockCode);
                return null;
            }
            
            Collections.reverse(klines);
            
            List<BigDecimal> closePrices = new ArrayList<>();
            for (KlineData kline : klines) {
                closePrices.add(kline.getClosePrice());
            }
            
            List<BigDecimal> shortMA = indicatorService.calculateMA(closePrices, shortPeriod);
            List<BigDecimal> longMA = indicatorService.calculateMA(closePrices, longPeriod);
            
            if (shortMA.size() < 2 || longMA.size() < 2) {
                return null;
            }
            
            BigDecimal currentPrice = closePrices.get(closePrices.size() - 1);
            
            if (indicatorService.isGoldenCross(shortMA, longMA)) {
                return createSignal(strategy, stockCode, stockName, "BUY", currentPrice,
                        String.format("MA%d上穿MA%d，形成金叉", shortPeriod, longPeriod),
                        buildIndicatorsMap("shortMA", shortMA.get(shortMA.size() - 1),
                                          "longMA", longMA.get(longMA.size() - 1)));
            }
            
            if (indicatorService.isDeathCross(shortMA, longMA)) {
                return createSignal(strategy, stockCode, stockName, "SELL", currentPrice,
                        String.format("MA%d下穿MA%d，形成死叉", shortPeriod, longPeriod),
                        buildIndicatorsMap("shortMA", shortMA.get(shortMA.size() - 1),
                                          "longMA", longMA.get(longMA.size() - 1)));
            }
            
        } catch (Exception e) {
            log.error("均线交叉策略分析失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * MACD策略分析
     * 买入信号：MACD线上穿信号线（金叉）且MACD在零轴上方
     * 卖出信号：MACD线下穿信号线（死叉）
     * 
     * @param strategy 策略配置
     * @param stockCode 股票代码
     * @param stockName 股票名称
     * @return 交易信号
     */
    private SignalEntity analyzeMACD(Strategy strategy, String stockCode, String stockName) {
        try {
            Map<String, Object> params = objectMapper.readValue(strategy.getParameters(), Map.class);
            String klinePeriod = (String) params.getOrDefault("klinePeriod", "1d");
            boolean requireAboveZero = (boolean) params.getOrDefault("requireAboveZero", true);
            
            List<KlineData> klines = klineDataMapper.findLatestKlines(stockCode, klinePeriod, 50);
            if (klines == null || klines.size() < 35) {
                log.debug("股票{}K线数据不足，跳过MACD分析", stockCode);
                return null;
            }
            
            Collections.reverse(klines);
            
            List<BigDecimal> closePrices = new ArrayList<>();
            for (KlineData kline : klines) {
                closePrices.add(kline.getClosePrice());
            }
            
            Map<String, List<BigDecimal>> macdData = indicatorService.calculateMACD(closePrices);
            List<BigDecimal> macdLine = macdData.get("macd");
            List<BigDecimal> signalLine = macdData.get("signal");
            List<BigDecimal> histogram = macdData.get("histogram");
            
            if (macdLine == null || signalLine == null || macdLine.size() < 2 || signalLine.size() < 2) {
                return null;
            }
            
            BigDecimal currentPrice = closePrices.get(closePrices.size() - 1);
            BigDecimal currentMACD = macdLine.get(macdLine.size() - 1);
            
            if (indicatorService.isGoldenCross(macdLine, signalLine)) {
                if (!requireAboveZero || currentMACD.compareTo(BigDecimal.ZERO) > 0) {
                    return createSignal(strategy, stockCode, stockName, "BUY", currentPrice,
                            "MACD金叉" + (currentMACD.compareTo(BigDecimal.ZERO) > 0 ? "，位于零轴上方" : ""),
                            buildIndicatorsMap("macd", currentMACD,
                                              "signal", signalLine.get(signalLine.size() - 1),
                                              "histogram", histogram.get(histogram.size() - 1)));
                }
            }
            
            if (indicatorService.isDeathCross(macdLine, signalLine)) {
                return createSignal(strategy, stockCode, stockName, "SELL", currentPrice,
                        "MACD死叉",
                        buildIndicatorsMap("macd", currentMACD,
                                          "signal", signalLine.get(signalLine.size() - 1),
                                          "histogram", histogram.get(histogram.size() - 1)));
            }
            
        } catch (Exception e) {
            log.error("MACD策略分析失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * RSI策略分析
     * 买入信号：RSI低于超卖线（通常30）
     * 卖出信号：RSI高于超买线（通常70）
     * 
     * @param strategy 策略配置
     * @param stockCode 股票代码
     * @param stockName 股票名称
     * @return 交易信号
     */
    private SignalEntity analyzeRSI(Strategy strategy, String stockCode, String stockName) {
        try {
            Map<String, Object> params = objectMapper.readValue(strategy.getParameters(), Map.class);
            int period = ((Number) params.getOrDefault("period", 14)).intValue();
            String klinePeriod = (String) params.getOrDefault("klinePeriod", "1d");
            int oversoldThreshold = ((Number) params.getOrDefault("oversoldThreshold", 30)).intValue();
            int overboughtThreshold = ((Number) params.getOrDefault("overboughtThreshold", 70)).intValue();
            
            List<KlineData> klines = klineDataMapper.findLatestKlines(stockCode, klinePeriod, period + 20);
            if (klines == null || klines.size() < period + 2) {
                log.debug("股票{}K线数据不足，跳过RSI分析", stockCode);
                return null;
            }
            
            Collections.reverse(klines);
            
            List<BigDecimal> closePrices = new ArrayList<>();
            for (KlineData kline : klines) {
                closePrices.add(kline.getClosePrice());
            }
            
            List<BigDecimal> rsiValues = indicatorService.calculateRSI(closePrices, period);
            if (rsiValues == null || rsiValues.isEmpty()) {
                return null;
            }
            
            BigDecimal currentRSI = rsiValues.get(rsiValues.size() - 1);
            BigDecimal previousRSI = rsiValues.size() > 1 ? rsiValues.get(rsiValues.size() - 2) : currentRSI;
            BigDecimal currentPrice = closePrices.get(closePrices.size() - 1);
            
            if (previousRSI.compareTo(BigDecimal.valueOf(oversoldThreshold)) <= 0 &&
                currentRSI.compareTo(BigDecimal.valueOf(oversoldThreshold)) > 0) {
                return createSignal(strategy, stockCode, stockName, "BUY", currentPrice,
                        String.format("RSI从超卖区域回升，当前值: %.2f", currentRSI),
                        buildIndicatorsMap("rsi", currentRSI));
            }
            
            if (previousRSI.compareTo(BigDecimal.valueOf(overboughtThreshold)) >= 0 &&
                currentRSI.compareTo(BigDecimal.valueOf(overboughtThreshold)) < 0) {
                return createSignal(strategy, stockCode, stockName, "SELL", currentPrice,
                        String.format("RSI从超买区域回落，当前值: %.2f", currentRSI),
                        buildIndicatorsMap("rsi", currentRSI));
            }
            
        } catch (Exception e) {
            log.error("RSI策略分析失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 布林带策略分析
     * 买入信号：价格跌破下轨
     * 卖出信号：价格突破上轨
     * 
     * @param strategy 策略配置
     * @param stockCode 股票代码
     * @param stockName 股票名称
     * @return 交易信号
     */
    private SignalEntity analyzeBollinger(Strategy strategy, String stockCode, String stockName) {
        try {
            Map<String, Object> params = objectMapper.readValue(strategy.getParameters(), Map.class);
            int period = ((Number) params.getOrDefault("period", 20)).intValue();
            double k = ((Number) params.getOrDefault("k", 2.0)).doubleValue();
            String klinePeriod = (String) params.getOrDefault("klinePeriod", "1d");
            
            List<KlineData> klines = klineDataMapper.findLatestKlines(stockCode, klinePeriod, period + 5);
            if (klines == null || klines.size() < period + 2) {
                log.debug("股票{}K线数据不足，跳过布林带分析", stockCode);
                return null;
            }
            
            Collections.reverse(klines);
            
            List<BigDecimal> closePrices = new ArrayList<>();
            for (KlineData kline : klines) {
                closePrices.add(kline.getClosePrice());
            }
            
            Map<String, List<BigDecimal>> bollingerData = indicatorService.calculateBollingerBands(closePrices, period, k);
            List<BigDecimal> upperBand = bollingerData.get("upper");
            List<BigDecimal> middleBand = bollingerData.get("middle");
            List<BigDecimal> lowerBand = bollingerData.get("lower");
            
            if (upperBand == null || lowerBand == null || upperBand.size() < 2) {
                return null;
            }
            
            BigDecimal currentPrice = closePrices.get(closePrices.size() - 1);
            BigDecimal previousPrice = closePrices.get(closePrices.size() - 2);
            BigDecimal currentUpper = upperBand.get(upperBand.size() - 1);
            BigDecimal currentLower = lowerBand.get(lowerBand.size() - 1);
            BigDecimal currentMiddle = middleBand.get(middleBand.size() - 1);
            
            if (previousPrice.compareTo(currentLower) < 0 && currentPrice.compareTo(currentLower) >= 0) {
                return createSignal(strategy, stockCode, stockName, "BUY", currentPrice,
                        "价格从布林带下轨反弹",
                        buildIndicatorsMap("upper", currentUpper, "middle", currentMiddle, "lower", currentLower));
            }
            
            if (previousPrice.compareTo(currentUpper) > 0 && currentPrice.compareTo(currentUpper) <= 0) {
                return createSignal(strategy, stockCode, stockName, "SELL", currentPrice,
                        "价格从布林带上轨回落",
                        buildIndicatorsMap("upper", currentUpper, "middle", currentMiddle, "lower", currentLower));
            }
            
        } catch (Exception e) {
            log.error("布林带策略分析失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 创建交易信号对象
     * @param strategy 策略配置
     * @param stockCode 股票代码
     * @param stockName 股票名称
     * @param signalType 信号类型
     * @param price 价格
     * @param reason 原因
     * @param indicators 指标数据
     * @return 信号对象
     */
    private SignalEntity createSignal(Strategy strategy, String stockCode, String stockName,
                                       String signalType, BigDecimal price, String reason, Map<String, Object> indicators) {
        SignalEntity signal = new SignalEntity();
        signal.setUserId(strategy.getUserId());
        signal.setStrategyId(strategy.getId());
        signal.setStockCode(stockCode);
        signal.setStockName(stockName);
        signal.setSignalType(signalType);
        signal.setSignalStrength(BigDecimal.ONE);
        signal.setPrice(price);
        signal.setReason(reason);
        signal.setStatus("pending");
        signal.setCreatedAt(LocalDateTime.now());
        
        try {
            signal.setIndicators(objectMapper.writeValueAsString(indicators));
        } catch (Exception e) {
            log.error("序列化指标数据失败", e);
        }
        
        return signal;
    }
    
    /**
     * 构建指标数据Map
     * @param keyValues 键值对数组
     * @return 指标Map
     */
    private Map<String, Object> buildIndicatorsMap(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                map.put(keyValues[i].toString(), keyValues[i + 1]);
            }
        }
        return map;
    }
    
    /**
     * 保存信号到数据库
     * @param signal 信号对象
     */
    private void saveSignal(SignalEntity signal) {
        try {
            signalMapper.insert(signal);
            log.info("保存信号成功: {} {} {} @ {}", signal.getSignalType(), signal.getStockCode(), 
                    signal.getStockName(), signal.getPrice());
        } catch (Exception e) {
            log.error("保存信号失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 执行所有活跃策略
     * @param stockPool 股票池
     * @return 所有策略生成的信号
     */
    public List<SignalEntity> executeAllActiveStrategies(Map<String, String> stockPool) {
        List<SignalEntity> allSignals = new ArrayList<>();
        
        List<Strategy> activeStrategies = strategyMapper.findActiveStrategies();
        log.info("找到{}个活跃策略", activeStrategies.size());
        
        for (Strategy strategy : activeStrategies) {
            try {
                List<SignalEntity> signals = executeStrategy(strategy.getId(), stockPool);
                allSignals.addAll(signals);
            } catch (Exception e) {
                log.error("执行策略{}失败: {}", strategy.getName(), e.getMessage(), e);
            }
        }
        
        return allSignals;
    }
    
    /**
     * 计算信号强度（基于多个因素）
     * @param strategy 策略配置
     * @param stockCode 股票代码
     * @param baseSignal 基础信号
     * @return 信号强度（0-1）
     */
    public BigDecimal calculateSignalStrength(Strategy strategy, String stockCode, SignalEntity baseSignal) {
        BigDecimal strength = BigDecimal.ONE;
        
        try {
            List<KlineData> klines = klineDataMapper.findLatestKlines(stockCode, "1d", 20);
            if (klines != null && klines.size() >= 10) {
                Collections.reverse(klines);
                
                List<BigDecimal> closePrices = new ArrayList<>();
                for (KlineData kline : klines) {
                    closePrices.add(kline.getClosePrice());
                }
                
                List<BigDecimal> ma5 = indicatorService.calculateMA(closePrices, 5);
                List<BigDecimal> ma10 = indicatorService.calculateMA(closePrices, 10);
                List<BigDecimal> ma20 = indicatorService.calculateMA(closePrices, 20);
                
                if (!ma5.isEmpty() && !ma10.isEmpty() && !ma20.isEmpty()) {
                    BigDecimal currentPrice = closePrices.get(closePrices.size() - 1);
                    BigDecimal latestMA5 = ma5.get(ma5.size() - 1);
                    BigDecimal latestMA10 = ma10.get(ma10.size() - 1);
                    BigDecimal latestMA20 = ma20.get(ma20.size() - 1);
                    
                    int trendCount = 0;
                    if ("BUY".equals(baseSignal.getSignalType())) {
                        if (currentPrice.compareTo(latestMA5) > 0) trendCount++;
                        if (latestMA5.compareTo(latestMA10) > 0) trendCount++;
                        if (latestMA10.compareTo(latestMA20) > 0) trendCount++;
                    } else {
                        if (currentPrice.compareTo(latestMA5) < 0) trendCount++;
                        if (latestMA5.compareTo(latestMA10) < 0) trendCount++;
                        if (latestMA10.compareTo(latestMA20) < 0) trendCount++;
                    }
                    
                    strength = BigDecimal.valueOf(0.5 + trendCount * 0.1667);
                }
            }
        } catch (Exception e) {
            log.error("计算信号强度失败: {}", e.getMessage());
        }
        
        return strength.setScale(4, RoundingMode.HALF_UP);
    }
}
