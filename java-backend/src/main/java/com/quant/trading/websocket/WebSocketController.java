package com.quant.trading.websocket;

import cn.dev33.satoken.stp.StpUtil;
import com.quant.trading.entity.TickData;
import com.quant.trading.fetcher.MarketDataFetcher;
import com.quant.trading.service.AccountService;
import com.quant.trading.service.OrderService;
import com.quant.trading.service.PositionService;
import com.quant.trading.service.StockPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    
    @Autowired
    private StockPoolService stockPoolService;
    
    @Autowired
    private MarketDataBroadcaster broadcaster;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private MarketDataFetcher marketDataFetcher;
    
    @Autowired
    private PositionService positionService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AccountService accountService;

    @MessageMapping("/init")
    public void handleInit(@Payload Map<String, Object> payload) {
        logger.info("收到初始化请求");
        
        Long userId = null;
        if (StpUtil.isLogin()) {
            userId = StpUtil.getLoginIdAsLong();
        }
        
        Map<String, Object> initData = new HashMap<>();
        
        if (userId != null) {
            initData.put("stockPool", stockPoolService.getStockPoolMapByUserId(userId));
            try {
                initData.put("account", accountService.getAccountInfoByUserId(userId));
            } catch (Exception e) {
                logger.warn("获取账户信息失败: {}", e.getMessage());
            }
            try {
                initData.put("positions", positionService.getActivePositionsByUserId(userId));
            } catch (Exception e) {
                logger.warn("获取持仓信息失败: {}", e.getMessage());
            }
            try {
                initData.put("orders", orderService.getActiveOrdersByUserId(userId));
            } catch (Exception e) {
                logger.warn("获取订单信息失败: {}", e.getMessage());
            }
        } else {
            initData.put("stockPool", stockPoolService.getStockPoolMap());
            try {
                initData.put("account", accountService.getAccountInfo());
            } catch (Exception e) {
                logger.warn("获取账户信息失败: {}", e.getMessage());
            }
            try {
                initData.put("positions", positionService.getActivePositions());
            } catch (Exception e) {
                logger.warn("获取持仓信息失败: {}", e.getMessage());
            }
            try {
                initData.put("orders", orderService.getActiveOrders());
            } catch (Exception e) {
                logger.warn("获取订单信息失败: {}", e.getMessage());
            }
        }
        
        messagingTemplate.convertAndSend("/topic/init", initData);
    }

    @MessageMapping("/subscribe/{stockCode}")
    @SendTo("/topic/tick/{stockCode}")
    public String subscribeStock(@DestinationVariable String stockCode) {
        logger.info("订阅股票: {}", stockCode);
        return "Subscribed to " + stockCode;
    }
    
    @MessageMapping("/history/{stockCode}")
    public void getHistory(@DestinationVariable String stockCode) {
        logger.info("获取历史数据: {}", stockCode);
        List<TickData> history = broadcaster.getTickHistory(stockCode);
        messagingTemplate.convertAndSend("/topic/history/" + stockCode, history);
    }
    
    @MessageMapping("/history/all")
    public void getAllHistory() {
        logger.info("获取所有历史数据");
        messagingTemplate.convertAndSend("/topic/history/all", "requested");
    }
    
    @MessageMapping("/search")
    public void searchStock(@Payload Map<String, Object> payload) {
        String keyword = (String) payload.get("keyword");
        logger.info("搜索股票: {}", keyword);
        
        List<Map<String, String>> results = marketDataFetcher.searchStocks(keyword);
        messagingTemplate.convertAndSend("/topic/search", results);
    }
    
    @MessageMapping("/stock/add")
    public void addStock(@Payload Map<String, Object> payload) {
        String code = (String) payload.get("code");
        String name = (String) payload.get("name");
        logger.info("添加股票: {} - {}", code, name);
        
        boolean success;
        Map<String, String> stockPoolMap;
        
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            success = stockPoolService.addStockForUser(userId, code, name);
            stockPoolMap = stockPoolService.getStockPoolMapByUserId(userId);
            
            String message = success ? "股票添加成功: " + code : "股票已存在或添加失败: " + code;
            
            Map<String, Object> result = new HashMap<>();
            result.put("action", "add");
            result.put("success", success);
            result.put("message", message);
            result.put("stockPool", stockPoolMap);
            
            broadcaster.broadcastStockPoolUpdateForUser(userId, "add", code, name, stockPoolMap);
        } else {
            success = stockPoolService.addStock(code, name);
            stockPoolMap = stockPoolService.getStockPoolMap();
            
            String message = success ? "股票添加成功: " + code : "股票已存在或添加失败: " + code;
            
            Map<String, Object> result = new HashMap<>();
            result.put("action", "add");
            result.put("success", success);
            result.put("message", message);
            result.put("stockPool", stockPoolMap);
            
            messagingTemplate.convertAndSend("/topic/stockPool", result);
        }
    }
    
    @MessageMapping("/stock/remove")
    public void removeStock(@Payload Map<String, Object> payload) {
        String code = (String) payload.get("code");
        logger.info("移除股票: {}", code);
        
        boolean success;
        Map<String, String> stockPoolMap;
        
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            success = stockPoolService.removeStockForUser(userId, code);
            stockPoolMap = stockPoolService.getStockPoolMapByUserId(userId);
            
            String message = success ? "股票移除成功: " + code : "股票不存在或移除失败: " + code;
            
            Map<String, Object> result = new HashMap<>();
            result.put("action", "remove");
            result.put("success", success);
            result.put("message", message);
            result.put("stockPool", stockPoolMap);
            
            broadcaster.broadcastStockPoolUpdateForUser(userId, "remove", code, "", stockPoolMap);
        } else {
            success = stockPoolService.removeStock(code);
            stockPoolMap = stockPoolService.getStockPoolMap();
            
            String message = success ? "股票移除成功: " + code : "股票不存在或移除失败: " + code;
            
            Map<String, Object> result = new HashMap<>();
            result.put("action", "remove");
            result.put("success", success);
            result.put("message", message);
            result.put("stockPool", stockPoolMap);
            
            messagingTemplate.convertAndSend("/topic/stockPool", result);
        }
        
        if (success) {
            broadcaster.clearTickHistory(code);
        }
    }
}
