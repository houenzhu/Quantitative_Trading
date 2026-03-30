package com.quant.trading.controller;

import cn.dev33.satoken.stp.StpUtil;
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
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            List<Position> positions = positionService.getActivePositionsByUserId(userId);
            return Result.success(positions);
        }
        List<Position> positions = positionService.getActivePositions();
        return Result.success(positions);
    }
    
    @GetMapping("/{stockCode}")
    public Result<Position> getPositionByStockCode(@PathVariable String stockCode) {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            Position position = positionService.getPositionByUserIdAndStockCode(userId, stockCode);
            return Result.success(position);
        }
        Position position = positionService.getPositionByStockCode(stockCode);
        return Result.success(position);
    }
    
    @PostMapping("/open")
    public Result<Position> openPosition(
            @RequestParam String stockCode,
            @RequestParam String stockName,
            @RequestParam int quantity,
            @RequestParam BigDecimal price) {
        Position position;
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            position = positionService.openPositionForUser(userId, stockCode, stockName, quantity, price);
        } else {
            position = positionService.openPosition(stockCode, stockName, quantity, price);
        }
        return Result.success(position);
    }
    
    @PostMapping("/close")
    public Result<Position> closePosition(
            @RequestParam String stockCode,
            @RequestParam int quantity,
            @RequestParam BigDecimal price) {
        Position position;
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            position = positionService.closePositionForUser(userId, stockCode, quantity, price);
        } else {
            position = positionService.closePosition(stockCode, quantity, price);
        }
        return Result.success(position);
    }
}
