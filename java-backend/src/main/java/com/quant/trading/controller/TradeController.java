package com.quant.trading.controller;

import com.quant.trading.common.Result;
import com.quant.trading.entity.Trade;
import com.quant.trading.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/trade")
public class TradeController {
    
    @Autowired
    private TradeService tradeService;
    
    @GetMapping("/recent")
    public Result<List<Trade>> getRecentTrades(@RequestParam(defaultValue = "100") int limit) {
        List<Trade> trades = tradeService.getRecentTrades(limit);
        return Result.success(trades);
    }
    
    @GetMapping("/stock/{stockCode}")
    public Result<List<Trade>> getTradesByStockCode(@PathVariable String stockCode) {
        List<Trade> trades = tradeService.getTradesByStockCode(stockCode);
        return Result.success(trades);
    }
}
