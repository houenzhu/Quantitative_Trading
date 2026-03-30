package com.quant.trading.controller;

import com.quant.trading.common.Result;
import com.quant.trading.entity.TickData;
import com.quant.trading.fetcher.MarketDataFetcher;
import com.quant.trading.fetcher.USStockDataFetcher;
import com.quant.trading.trading.TradingHours;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketDataController {
    
    @Autowired
    private MarketDataFetcher marketDataFetcher;
    
    @GetMapping("/tick/{stockCode}")
    public Result<TickData> getTickData(@PathVariable String stockCode) {
        TickData tickData = marketDataFetcher.fetchTickData(stockCode);
        return Result.success(tickData);
    }
    
    @PostMapping("/tick/batch")
    public Result<List<TickData>> getBatchTickData(@RequestBody List<String> stockCodes) {
        List<TickData> tickDataList = marketDataFetcher.fetchBatchTickData(stockCodes);
        return Result.success(tickDataList);
    }
    
    @GetMapping("/status")
    public Result<Map<String, Object>> getMarketStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("aStock", getAStockStatus());
        status.put("usStock", getUSStockStatus());
        
        return Result.success(status);
    }
    
    private Map<String, Object> getAStockStatus() {
        Map<String, Object> aStock = new HashMap<>();
        
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        int hour = now.getHour();
        int minute = now.getMinute();
        int currentTime = hour * 60 + minute;
        
        int morningOpen = 9 * 60 + 30;
        int morningClose = 11 * 60 + 30;
        int afternoonOpen = 13 * 60;
        int afternoonClose = 15 * 60;
        
        String status;
        boolean isTrading = false;
        
        if (currentTime < morningOpen) {
            status = "休市";
        } else if (currentTime < morningClose) {
            status = "早盘交易中";
            isTrading = true;
        } else if (currentTime < afternoonOpen) {
            status = "午间休市";
        } else if (currentTime < afternoonClose) {
            status = "午盘交易中";
            isTrading = true;
        } else {
            status = "休市";
        }
        
        aStock.put("status", status);
        aStock.put("isTrading", isTrading);
        aStock.put("currentTime", now.toLocalDateTime().toString());
        aStock.put("timezone", "Asia/Shanghai");
        
        return aStock;
    }
    
    private Map<String, Object> getUSStockStatus() {
        Map<String, Object> usStock = new HashMap<>();
        
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        int hour = now.getHour();
        int minute = now.getMinute();
        int currentTime = hour * 60 + minute;
        
        int preMarket = 4 * 60;
        int marketOpen = 9 * 60 + 30;
        int marketClose = 16 * 60;
        int afterMarket = 20 * 60;
        
        String status;
        boolean isTrading = false;
        
        if (currentTime < preMarket) {
            status = "休市";
        } else if (currentTime < marketOpen) {
            status = "盘前交易";
        } else if (currentTime < marketClose) {
            status = "交易中";
            isTrading = true;
        } else if (currentTime < afterMarket) {
            status = "盘后交易";
        } else {
            status = "休市";
        }
        
        usStock.put("status", status);
        usStock.put("isTrading", isTrading);
        usStock.put("currentTime", now.toLocalDateTime().toString());
        usStock.put("timezone", "America/New_York");
        
        return usStock;
    }
}
