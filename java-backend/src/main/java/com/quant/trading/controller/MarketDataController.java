package com.quant.trading.controller;

import com.quant.trading.common.Result;
import com.quant.trading.entity.TickData;
import com.quant.trading.fetcher.MarketDataFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
}
