package com.quant.trading.scheduler;

import com.quant.trading.entity.TickData;
import com.quant.trading.fetcher.MarketDataFetcher;
import com.quant.trading.service.PositionService;
import com.quant.trading.service.StockPoolService;
import com.quant.trading.trading.TradingHours;
import com.quant.trading.websocket.MarketDataBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketDataScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataScheduler.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @Autowired
    private MarketDataFetcher marketDataFetcher;
    
    @Autowired
    private StockPoolService stockPoolService;
    
    @Autowired
    private PositionService positionService;
    
    @Autowired
    private MarketDataBroadcaster broadcaster;
    
    @Autowired
    private TradingHours tradingHours;
    
    @Autowired
    private RealtimeStrategyExecutor realtimeStrategyExecutor;
    
    private final Map<String, List<TickData>> tickHistoryMap = new ConcurrentHashMap<>();
    
    private boolean loggedNonTradingTime = false;
    
    @Scheduled(fixedRate = 3000)
    public void fetchAndBroadcastMarketData() {
        LocalDateTime now = LocalDateTime.now();
        boolean isTradingTime = tradingHours.isTradingTime(now);
        
        if (!isTradingTime) {
            if (!loggedNonTradingTime) {
                logger.info("当前非交易时间: {}，暂停行情获取", now.format(TIME_FORMAT));
                loggedNonTradingTime = true;
            }
            return;
        }
        loggedNonTradingTime = false;
        
        Map<String, String> stockPool = stockPoolService.getStockPoolMap();
        
        if (stockPool.isEmpty()) {
            logger.warn("股票池为空，无法获取行情");
            return;
        }
        
        List<String> stockCodes = new ArrayList<>(stockPool.keySet());
        List<TickData> tickDataList = marketDataFetcher.fetchBatchTickData(stockCodes);
        
        if (tickDataList.isEmpty()) {
            logger.warn("未获取到任何行情数据");
            return;
        }
        
        for (TickData tick : tickDataList) {
            if (tick != null && tick.getCode() != null) {
                String stockCode = tick.getCode();
                tick.setName(stockPool.get(stockCode));
                
                tickHistoryMap.computeIfAbsent(stockCode, k -> new ArrayList<>());
                tickHistoryMap.get(stockCode).add(tick);
                
                if (tickHistoryMap.get(stockCode).size() > 1000) {
                    tickHistoryMap.get(stockCode).remove(0);
                }
                
                broadcaster.broadcastTickData(tick);
                
                if (tick.getPrice() != null) {
                    positionService.updatePositionPrice(stockCode, tick.getPrice());
                }
                
                realtimeStrategyExecutor.onTickData(tick, stockPool);
            }
        }
        
        logger.info("获取并广播行情数据: {} 只股票 - {}", tickDataList.size(), now.format(TIME_FORMAT));
    }
    
    public List<TickData> getTickHistory(String stockCode) {
        return tickHistoryMap.getOrDefault(stockCode, new ArrayList<>());
    }
    
    public void clearTickHistory(String stockCode) {
        tickHistoryMap.remove(stockCode);
    }
}
