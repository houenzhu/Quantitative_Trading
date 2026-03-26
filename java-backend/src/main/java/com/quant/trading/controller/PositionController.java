package com.quant.trading.controller;

import com.quant.trading.common.Result;
import com.quant.trading.entity.Position;
import com.quant.trading.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/position")
public class PositionController {
    
    @Autowired
    private PositionService positionService;
    
    @GetMapping("/active")
    public Result<List<Position>> getActivePositions() {
        List<Position> positions = positionService.getActivePositions();
        return Result.success(positions);
    }
    
    @GetMapping("/{stockCode}")
    public Result<Position> getPositionByStockCode(@PathVariable String stockCode) {
        Position position = positionService.getPositionByStockCode(stockCode);
        return Result.success(position);
    }
    
    @PostMapping("/open")
    public Result<Position> openPosition(
            @RequestParam String stockCode,
            @RequestParam String stockName,
            @RequestParam int quantity,
            @RequestParam BigDecimal price) {
        Position position = positionService.openPosition(stockCode, stockName, quantity, price);
        return Result.success(position);
    }
    
    @PostMapping("/close")
    public Result<Position> closePosition(
            @RequestParam String stockCode,
            @RequestParam int quantity,
            @RequestParam BigDecimal price) {
        Position position = positionService.closePosition(stockCode, quantity, price);
        return Result.success(position);
    }
}
