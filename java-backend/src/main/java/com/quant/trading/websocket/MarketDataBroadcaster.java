package com.quant.trading.websocket;

import com.alibaba.fastjson2.JSON;
import com.quant.trading.entity.TickData;
import com.quant.trading.service.StockPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MarketDataBroadcaster {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataBroadcaster.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private StockPoolService stockPoolService;
    
    private final Map<String, List<TickData>> tickHistory = new ConcurrentHashMap<>();
    
    public void broadcastTickData(TickData tickData) {
        String stockCode = tickData.getCode();
        
        tickHistory.computeIfAbsent(stockCode, k -> new CopyOnWriteArrayList<>());
        List<TickData> history = tickHistory.get(stockCode);
        history.add(tickData);
        
        if (history.size() > 1000) {
            history.remove(0);
        }
        
        messagingTemplate.convertAndSend("/topic/tick/" + stockCode, tickData);
        
        messagingTemplate.convertAndSend("/topic/market", tickData);
        
        logger.info("广播行情数据: {} - {} - {}", stockCode, tickData.getName(), tickData.getPrice());
    }
    
    public void broadcastAllTickData(Map<String, TickData> tickDataMap) {
        messagingTemplate.convertAndSend("/topic/market", tickDataMap);
    }
    
    public void broadcastStockPoolUpdate(String action, String stockCode, String stockName) {
        Map<String, Object> message = Map.of(
            "action", action,
            "stockCode", stockCode,
            "stockName", stockName,
            "stockPool", stockPoolService.getStockPoolMap()
        );
        messagingTemplate.convertAndSend("/topic/stockPool", message);
    }
    
    public void broadcastOrderUpdate(Object order) {
        messagingTemplate.convertAndSend("/topic/orders", order);
    }
    
    public void broadcastTradeUpdate(Object trade) {
        messagingTemplate.convertAndSend("/topic/trades", trade);
    }
    
    public void broadcastPositionUpdate(Object position) {
        messagingTemplate.convertAndSend("/topic/positions", position);
    }
    
    public void broadcastAccountUpdate(Object account) {
        messagingTemplate.convertAndSend("/topic/account", account);
    }
    
    public void broadcastSignal(Object signal) {
        messagingTemplate.convertAndSend("/topic/signals", signal);
    }
    
    public List<TickData> getTickHistory(String stockCode) {
        return tickHistory.getOrDefault(stockCode, new CopyOnWriteArrayList<>());
    }
    
    public void clearTickHistory(String stockCode) {
        tickHistory.remove(stockCode);
    }
    
    public void clearAllTickHistory() {
        tickHistory.clear();
    }
}
