package com.quant.trading.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技术指标计算服务
 * 提供各种常用技术指标的计算方法
 * 包括: MA、EMA、MACD、RSI、布林带、KDJ等
 */
@Service
@Slf4j
public class TechnicalIndicatorService {
    
    /**
     * 计算简单移动平均线
     * @param prices 价格列表(按时间升序)
     * @param period 周期
     * @return MA值列表
     */
    public List<BigDecimal> calculateMA(List<BigDecimal> prices, int period) {
        List<BigDecimal> maValues = new ArrayList<>();
        
        if (prices == null || prices.size() < period) {
            log.warn("价格数据不足，无法计算MA{}，需要{}个数据点，实际{}", period, period, prices != null ? prices.size() : 0);
            return maValues;
        }
        
        for (int i = period - 1; i < prices.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                sum = sum.add(prices.get(j));
            }
            BigDecimal ma = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            maValues.add(ma);
        }
        
        return maValues;
    }
    
    /**
     * 计算指数移动平均线
     * @param prices 价格列表(按时间升序)
     * @param period 周期
     * @return EMA值列表
     */
    public List<BigDecimal> calculateEMA(List<BigDecimal> prices, int period) {
        List<BigDecimal> emaValues = new ArrayList<>();
        
        if (prices == null || prices.isEmpty()) {
            return emaValues;
        }
        
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < Math.min(period, prices.size()); i++) {
            sum = sum.add(prices.get(i));
        }
        BigDecimal firstEMA = sum.divide(BigDecimal.valueOf(Math.min(period, prices.size())), 4, RoundingMode.HALF_UP);
        emaValues.add(firstEMA);
        
        for (int i = Math.min(period, prices.size()); i < prices.size(); i++) {
            BigDecimal ema = prices.get(i).multiply(multiplier)
                    .add(emaValues.get(emaValues.size() - 1).multiply(BigDecimal.ONE.subtract(multiplier)));
            emaValues.add(ema.setScale(4, RoundingMode.HALF_UP));
        }
        
        return emaValues;
    }
    
    /**
     * 计算MACD指标
     * MACD = EMA(12) - EMA(26)
     * Signal = EMA(MACD, 9)
     * Histogram = MACD - Signal
     * 
     * @param prices 价格列表(按时间升序)
     * @return 包含MACD、Signal、Histogram的Map
     */
    public Map<String, List<BigDecimal>> calculateMACD(List<BigDecimal> prices) {
        Map<String, List<BigDecimal>> result = new HashMap<>();
        
        if (prices == null || prices.size() < 26) {
            log.warn("价格数据不足，无法计算MACD，需要至少26个数据点");
            result.put("macd", new ArrayList<>());
            result.put("signal", new ArrayList<>());
            result.put("histogram", new ArrayList<>());
            return result;
        }
        
        List<BigDecimal> ema12 = calculateEMA(prices, 12);
        List<BigDecimal> ema26 = calculateEMA(prices, 26);
        
        int diff = ema26.size() - ema12.size();
        List<BigDecimal> macdLine = new ArrayList<>();
        for (int i = 0; i < ema26.size(); i++) {
            int ema12Index = i + diff;
            if (ema12Index >= 0 && ema12Index < ema12.size()) {
                BigDecimal macd = ema12.get(ema12Index).subtract(ema26.get(i));
                macdLine.add(macd);
            }
        }
        
        List<BigDecimal> signalLine = calculateEMA(macdLine, 9);
        
        List<BigDecimal> histogram = new ArrayList<>();
        int signalDiff = macdLine.size() - signalLine.size();
        for (int i = 0; i < signalLine.size(); i++) {
            int macdIndex = i + signalDiff;
            if (macdIndex >= 0 && macdIndex < macdLine.size()) {
                BigDecimal hist = macdLine.get(macdIndex).subtract(signalLine.get(i));
                histogram.add(hist);
            }
        }
        
        result.put("macd", macdLine);
        result.put("signal", signalLine);
        result.put("histogram", histogram);
        
        return result;
    }
    
    /**
     * 计算RSI相对强弱指标
     * RSI = 100 - 100 / (1 + RS)
     * RS = 平均上涨幅度 / 平均下跌幅度
     * 
     * @param prices 价格列表(按时间升序)
     * @param period 周期(通常为14)
     * @return RSI值列表
     */
    public List<BigDecimal> calculateRSI(List<BigDecimal> prices, int period) {
        List<BigDecimal> rsiValues = new ArrayList<>();
        
        if (prices == null || prices.size() < period + 1) {
            log.warn("价格数据不足，无法计算RSI{}，需要{}个数据点", period, period + 1);
            return rsiValues;
        }
        
        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();
        
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains.add(change);
                losses.add(BigDecimal.ZERO);
            } else {
                gains.add(BigDecimal.ZERO);
                losses.add(change.abs());
            }
        }
        
        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;
        
        for (int i = 0; i < period; i++) {
            avgGain = avgGain.add(gains.get(i));
            avgLoss = avgLoss.add(losses.get(i));
        }
        avgGain = avgGain.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        
        for (int i = period; i < gains.size(); i++) {
            if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
                rsiValues.add(BigDecimal.valueOf(100));
            } else {
                BigDecimal rs = avgGain.divide(avgLoss, 4, RoundingMode.HALF_UP);
                BigDecimal rsi = BigDecimal.valueOf(100)
                        .subtract(BigDecimal.valueOf(100)
                                .divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP));
                rsiValues.add(rsi);
            }
            
            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1))
                    .add(gains.get(i))
                    .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1))
                    .add(losses.get(i))
                    .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        }
        
        return rsiValues;
    }
    
    /**
     * 计算布林带指标
     * 中轨 = MA(period)
     * 上轨 = 中轨 + k * 标准差
     * 下轨 = 中轨 - k * 标准差
     * 
     * @param prices 价格列表(按时间升序)
     * @param period 周期(通常为20)
     * @param k 标准差倍数(通常为2)
     * @return 包含上轨、中轨、下轨的Map
     */
    public Map<String, List<BigDecimal>> calculateBollingerBands(List<BigDecimal> prices, int period, double k) {
        Map<String, List<BigDecimal>> result = new HashMap<>();
        
        if (prices == null || prices.size() < period) {
            log.warn("价格数据不足，无法计算布林带，需要{}个数据点", period);
            result.put("upper", new ArrayList<>());
            result.put("middle", new ArrayList<>());
            result.put("lower", new ArrayList<>());
            return result;
        }
        
        List<BigDecimal> middleBand = calculateMA(prices, period);
        List<BigDecimal> upperBand = new ArrayList<>();
        List<BigDecimal> lowerBand = new ArrayList<>();
        
        for (int i = period - 1; i < prices.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                BigDecimal diff = prices.get(j).subtract(middleBand.get(i - period + 1));
                sum = sum.add(diff.multiply(diff));
            }
            BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP).doubleValue()));
            
            upperBand.add(middleBand.get(i - period + 1).add(stdDev.multiply(BigDecimal.valueOf(k))));
            lowerBand.add(middleBand.get(i - period + 1).subtract(stdDev.multiply(BigDecimal.valueOf(k))));
        }
        
        result.put("upper", upperBand);
        result.put("middle", middleBand);
        result.put("lower", lowerBand);
        
        return result;
    }
    
    /**
     * 计算KDJ指标
     * RSV = (收盘价 - N日最低价) / (N日最高价 - N日最低价) * 100
     * K = 2/3 * 前一日K + 1/3 * RSV
     * D = 2/3 * 前一日D + 1/3 * K
     * J = 3 * K - 2 * D
     * 
     * @param highPrices 最高价列表
     * @param lowPrices 最低价列表
     * @param closePrices 收盘价列表
     * @param period 周期(通常为9)
     * @return 包含K、D、J值的Map
     */
    public Map<String, List<BigDecimal>> calculateKDJ(List<BigDecimal> highPrices, 
                                                       List<BigDecimal> lowPrices, 
                                                       List<BigDecimal> closePrices, 
                                                       int period) {
        Map<String, List<BigDecimal>> result = new HashMap<>();
        
        if (highPrices == null || lowPrices == null || closePrices == null ||
            highPrices.size() < period || lowPrices.size() < period || closePrices.size() < period) {
            log.warn("价格数据不足，无法计算KDJ");
            result.put("k", new ArrayList<>());
            result.put("d", new ArrayList<>());
            result.put("j", new ArrayList<>());
            return result;
        }
        
        List<BigDecimal> rsvList = new ArrayList<>();
        
        for (int i = period - 1; i < closePrices.size(); i++) {
            BigDecimal highestHigh = highPrices.get(i);
            BigDecimal lowestLow = lowPrices.get(i);
            
            for (int j = i - period + 1; j < i; j++) {
                if (highPrices.get(j).compareTo(highestHigh) > 0) {
                    highestHigh = highPrices.get(j);
                }
                if (lowPrices.get(j).compareTo(lowestLow) < 0) {
                    lowestLow = lowPrices.get(j);
                }
            }
            
            BigDecimal rsv;
            BigDecimal range = highestHigh.subtract(lowestLow);
            if (range.compareTo(BigDecimal.ZERO) == 0) {
                rsv = BigDecimal.valueOf(50);
            } else {
                rsv = closePrices.get(i).subtract(lowestLow)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(range, 4, RoundingMode.HALF_UP);
            }
            rsvList.add(rsv);
        }
        
        List<BigDecimal> kValues = new ArrayList<>();
        List<BigDecimal> dValues = new ArrayList<>();
        List<BigDecimal> jValues = new ArrayList<>();
        
        BigDecimal prevK = BigDecimal.valueOf(50);
        BigDecimal prevD = BigDecimal.valueOf(50);
        
        for (BigDecimal rsv : rsvList) {
            BigDecimal k = prevK.multiply(BigDecimal.valueOf(2.0 / 3.0))
                    .add(rsv.multiply(BigDecimal.valueOf(1.0 / 3.0)))
                    .setScale(4, RoundingMode.HALF_UP);
            
            BigDecimal d = prevD.multiply(BigDecimal.valueOf(2.0 / 3.0))
                    .add(k.multiply(BigDecimal.valueOf(1.0 / 3.0)))
                    .setScale(4, RoundingMode.HALF_UP);
            
            BigDecimal j = k.multiply(BigDecimal.valueOf(3))
                    .subtract(d.multiply(BigDecimal.valueOf(2)))
                    .setScale(4, RoundingMode.HALF_UP);
            
            kValues.add(k);
            dValues.add(d);
            jValues.add(j);
            
            prevK = k;
            prevD = d;
        }
        
        result.put("k", kValues);
        result.put("d", dValues);
        result.put("j", jValues);
        
        return result;
    }
    
    /**
     * 计算成交量加权平均价
     * VWAP = 累计(价格 * 成交量) / 累计成交量
     * 
     * @param prices 价格列表
     * @param volumes 成交量列表
     * @return VWAP值列表
     */
    public List<BigDecimal> calculateVWAP(List<BigDecimal> prices, List<Long> volumes) {
        List<BigDecimal> vwapValues = new ArrayList<>();
        
        if (prices == null || volumes == null || prices.size() != volumes.size() || prices.isEmpty()) {
            return vwapValues;
        }
        
        BigDecimal cumulativePV = BigDecimal.ZERO;
        long cumulativeVolume = 0;
        
        for (int i = 0; i < prices.size(); i++) {
            cumulativePV = cumulativePV.add(prices.get(i).multiply(BigDecimal.valueOf(volumes.get(i))));
            cumulativeVolume += volumes.get(i);
            
            if (cumulativeVolume > 0) {
                BigDecimal vwap = cumulativePV.divide(BigDecimal.valueOf(cumulativeVolume), 4, RoundingMode.HALF_UP);
                vwapValues.add(vwap);
            }
        }
        
        return vwapValues;
    }
    
    /**
     * 获取最新的指标值
     * @param indicatorList 指标列表
     * @return 最新值，如果列表为空返回null
     */
    public BigDecimal getLatestValue(List<BigDecimal> indicatorList) {
        if (indicatorList == null || indicatorList.isEmpty()) {
            return null;
        }
        return indicatorList.get(indicatorList.size() - 1);
    }
    
    /**
     * 判断是否发生金叉
     * @param fastLine 快线
     * @param slowLine 慢线
     * @return true表示发生金叉
     */
    public boolean isGoldenCross(List<BigDecimal> fastLine, List<BigDecimal> slowLine) {
        if (fastLine == null || slowLine == null || fastLine.size() < 2 || slowLine.size() < 2) {
            return false;
        }
        
        int fastSize = fastLine.size();
        int slowSize = slowLine.size();
        
        BigDecimal fastCurrent = fastLine.get(fastSize - 1);
        BigDecimal fastPrevious = fastLine.get(fastSize - 2);
        BigDecimal slowCurrent = slowLine.get(slowSize - 1);
        BigDecimal slowPrevious = slowLine.get(slowSize - 2);
        
        return fastPrevious.compareTo(slowPrevious) <= 0 && fastCurrent.compareTo(slowCurrent) > 0;
    }
    
    /**
     * 判断是否发生死叉
     * @param fastLine 快线
     * @param slowLine 慢线
     * @return true表示发生死叉
     */
    public boolean isDeathCross(List<BigDecimal> fastLine, List<BigDecimal> slowLine) {
        if (fastLine == null || slowLine == null || fastLine.size() < 2 || slowLine.size() < 2) {
            return false;
        }
        
        int fastSize = fastLine.size();
        int slowSize = slowLine.size();
        
        BigDecimal fastCurrent = fastLine.get(fastSize - 1);
        BigDecimal fastPrevious = fastLine.get(fastSize - 2);
        BigDecimal slowCurrent = slowLine.get(slowSize - 1);
        BigDecimal slowPrevious = slowLine.get(slowSize - 2);
        
        return fastPrevious.compareTo(slowPrevious) >= 0 && fastCurrent.compareTo(slowCurrent) < 0;
    }
}
