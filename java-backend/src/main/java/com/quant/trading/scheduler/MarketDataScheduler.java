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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketDataScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataScheduler.class);
    
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
    
    private final Map<String, List<TickData>> tickHistoryMap = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 3000)
    public void fetchAndBroadcastMarketData() {
        if (!tradingHours.isTradingTime(LocalDateTime.now())) {
            return;
        }
        
        Map<String, String> stockPool = stockPoolService.getStockPoolMap();
        
        if (stockPool.isEmpty()) {
            return;
        }
        
        List<String> stockCodes = new ArrayList<>(stockPool.keySet());
        List<TickData> tickDataList = marketDataFetcher.fetchBatchTickData(stockCodes);
        
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
            }
        }
        
        logger.debug("获取并广播行情数据: {} 只股票", tickDataList.size());
    }
    
    public List<TickData> getTickHistory(String stockCode) {
        return tickHistoryMap.getOrDefault(stockCode, new ArrayList<>());
    }
    
    public void clearTickHistory(String stockCode) {
        tickHistoryMap.remove(stockCode);
    }
}
