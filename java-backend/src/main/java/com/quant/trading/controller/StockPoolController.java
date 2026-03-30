package com.quant.trading.controller;

import cn.dev33.satoken.stp.StpUtil;
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
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            Map<String, String> pool = stockPoolService.getStockPoolMapByUserId(userId);
            return Result.success(pool);
        }
        Map<String, String> pool = stockPoolService.getStockPoolMap();
        return Result.success(pool);
    }
    
    @GetMapping("/pool/list")
    public Result<List<StockPoolItem>> getStockPoolList() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            List<StockPoolItem> items = stockPoolService.getActiveStocksByUserId(userId);
            return Result.success(items);
        }
        List<StockPoolItem> items = stockPoolService.getActiveStocks();
        return Result.success(items);
    }
    
    @PostMapping("/pool/add")
    public Result<Boolean> addStock(
            @RequestParam String stockCode,
            @RequestParam String stockName) {
        boolean added;
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            added = stockPoolService.addStockForUser(userId, stockCode, stockName);
        } else {
            added = stockPoolService.addStock(stockCode, stockName);
        }
        if (added) {
            broadcaster.broadcastStockPoolUpdate("add", stockCode, stockName);
        }
        return Result.success(added);
    }
    
    @PostMapping("/pool/remove")
    public Result<Boolean> removeStock(@RequestParam String stockCode) {
        boolean removed;
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            removed = stockPoolService.removeStockForUser(userId, stockCode);
        } else {
            removed = stockPoolService.removeStock(stockCode);
        }
        if (removed) {
            broadcaster.broadcastStockPoolUpdate("remove", stockCode, null);
        }
        return Result.success(removed);
    }
    
    @GetMapping("/pool/check/{stockCode}")
    public Result<Boolean> isInPool(@PathVariable String stockCode) {
        boolean inPool;
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            inPool = stockPoolService.isInPoolForUser(userId, stockCode);
        } else {
            inPool = stockPoolService.isInPool(stockCode);
        }
        return Result.success(inPool);
    }
}
