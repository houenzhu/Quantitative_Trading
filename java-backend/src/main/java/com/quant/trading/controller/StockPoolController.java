package com.quant.trading.controller;

import com.quant.trading.common.Result;
import com.quant.trading.entity.StockPoolItem;
import com.quant.trading.service.StockPoolService;
import com.quant.trading.websocket.MarketDataBroadcaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockPoolController {
    
    @Autowired
    private StockPoolService stockPoolService;
    
    @Autowired
    private MarketDataBroadcaster broadcaster;
    
    @GetMapping("/pool")
    public Result<Map<String, String>> getStockPool() {
        Map<String, String> pool = stockPoolService.getStockPoolMap();
        return Result.success(pool);
    }
    
    @GetMapping("/pool/list")
    public Result<List<StockPoolItem>> getStockPoolList() {
        List<StockPoolItem> items = stockPoolService.getActiveStocks();
        return Result.success(items);
    }
    
    @PostMapping("/pool/add")
    public Result<Boolean> addStock(
            @RequestParam String stockCode,
            @RequestParam String stockName) {
        boolean added = stockPoolService.addStock(stockCode, stockName);
        if (added) {
            broadcaster.broadcastStockPoolUpdate("add", stockCode, stockName);
        }
        return Result.success(added);
    }
    
    @PostMapping("/pool/remove")
    public Result<Boolean> removeStock(@RequestParam String stockCode) {
        boolean removed = stockPoolService.removeStock(stockCode);
        if (removed) {
            broadcaster.broadcastStockPoolUpdate("remove", stockCode, null);
        }
        return Result.success(removed);
    }
    
    @GetMapping("/pool/check/{stockCode}")
    public Result<Boolean> isInPool(@PathVariable String stockCode) {
        boolean inPool = stockPoolService.isInPool(stockCode);
        return Result.success(inPool);
    }
}
