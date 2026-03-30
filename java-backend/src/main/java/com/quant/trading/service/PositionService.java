package com.quant.trading.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quant.trading.entity.Position;
import com.quant.trading.mapper.PositionMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PositionService extends ServiceImpl<PositionMapper, Position> {
    
    public List<Position> getActivePositions() {
        return baseMapper.findActivePositions();
    }
    
    public List<Position> getActivePositionsByUserId(Long userId) {
        return baseMapper.findActivePositionsByUserId(userId);
    }
    
    public List<Position> getAllPositions() {
        return list();
    }
    
    public Position getPositionByStockCode(String stockCode) {
        return baseMapper.findByStockCode(stockCode);
    }
    
    public Position getPositionByUserIdAndStockCode(Long userId, String stockCode) {
        return baseMapper.findByUserIdAndStockCode(userId, stockCode);
    }
    
    public Position openPosition(String stockCode, String stockName, int quantity, BigDecimal price) {
        Position position = getPositionByStockCode(stockCode);
        
        if (position != null) {
            int newQuantity = position.getQuantity() + quantity;
            BigDecimal totalCost = position.getAvgCost().multiply(BigDecimal.valueOf(position.getQuantity()))
                    .add(price.multiply(BigDecimal.valueOf(quantity)));
            BigDecimal newAvgCost = totalCost.divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);
            
            position.setQuantity(newQuantity);
            position.setAvgCost(newAvgCost);
            position.setUpdatedAt(LocalDateTime.now());
            updateById(position);
        } else {
            position = new Position();
            position.setStockCode(stockCode);
            position.setStockName(stockName);
            position.setQuantity(quantity);
            position.setAvailableQuantity(0);
            position.setAvgCost(price);
            position.setCurrentPrice(price);
            position.setMarketValue(price.multiply(BigDecimal.valueOf(quantity)));
            position.setUnrealizedPnl(BigDecimal.ZERO);
            position.setUnrealizedPnlPct(BigDecimal.ZERO);
            position.setIsActive(true);
            save(position);
        }
        
        return position;
    }
    
    public Position openPositionForUser(Long userId, String stockCode, String stockName, int quantity, BigDecimal price) {
        Position position = getPositionByUserIdAndStockCode(userId, stockCode);
        
        if (position != null) {
            int newQuantity = position.getQuantity() + quantity;
            BigDecimal totalCost = position.getAvgCost().multiply(BigDecimal.valueOf(position.getQuantity()))
                    .add(price.multiply(BigDecimal.valueOf(quantity)));
            BigDecimal newAvgCost = totalCost.divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);
            
            position.setQuantity(newQuantity);
            position.setAvgCost(newAvgCost);
            position.setUpdatedAt(LocalDateTime.now());
            updateById(position);
        } else {
            position = new Position();
            position.setUserId(userId);
            position.setStockCode(stockCode);
            position.setStockName(stockName);
            position.setQuantity(quantity);
            position.setAvailableQuantity(0);
            position.setAvgCost(price);
            position.setCurrentPrice(price);
            position.setMarketValue(price.multiply(BigDecimal.valueOf(quantity)));
            position.setUnrealizedPnl(BigDecimal.ZERO);
            position.setUnrealizedPnlPct(BigDecimal.ZERO);
            position.setIsActive(true);
            save(position);
        }
        
        return position;
    }
    
    public Position closePosition(String stockCode, int quantity, BigDecimal price) {
        Position position = getPositionByStockCode(stockCode);
        
        if (position == null) {
            return null;
        }
        
        int remainingQuantity = position.getQuantity() - quantity;
        
        if (remainingQuantity <= 0) {
            baseMapper.deactivateByStockCode(stockCode);
            position.setIsActive(false);
            position.setClosedAt(LocalDateTime.now());
        } else {
            position.setQuantity(remainingQuantity);
            position.setUpdatedAt(LocalDateTime.now());
            updateById(position);
        }
        
        return position;
    }
    
    public Position closePositionForUser(Long userId, String stockCode, int quantity, BigDecimal price) {
        Position position = getPositionByUserIdAndStockCode(userId, stockCode);
        
        if (position == null) {
            return null;
        }
        
        int remainingQuantity = position.getQuantity() - quantity;
        
        if (remainingQuantity <= 0) {
            position.setQuantity(0);
            position.setIsActive(false);
            position.setClosedAt(LocalDateTime.now());
            baseMapper.deactivateByUserIdAndStockCode(userId, stockCode);
        } else {
            position.setQuantity(remainingQuantity);
            position.setUpdatedAt(LocalDateTime.now());
            updateById(position);
        }
        
        return position;
    }
    
    public void updatePositionPrice(String stockCode, BigDecimal currentPrice) {
        Position position = getPositionByStockCode(stockCode);
        if (position != null && position.getIsActive()) {
            position.setCurrentPrice(currentPrice);
            position.setMarketValue(currentPrice.multiply(BigDecimal.valueOf(position.getQuantity())));
            
            BigDecimal cost = position.getAvgCost().multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal unrealizedPnl = position.getMarketValue().subtract(cost);
            position.setUnrealizedPnl(unrealizedPnl);
            
            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unrealizedPnlPct = unrealizedPnl.divide(cost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                position.setUnrealizedPnlPct(unrealizedPnlPct);
            }
            
            position.setUpdatedAt(LocalDateTime.now());
            updateById(position);
        }
    }
    
    public void updatePositionPriceForUser(Long userId, String stockCode, BigDecimal currentPrice) {
        Position position = getPositionByUserIdAndStockCode(userId, stockCode);
        if (position != null && position.getIsActive()) {
            position.setCurrentPrice(currentPrice);
            position.setMarketValue(currentPrice.multiply(BigDecimal.valueOf(position.getQuantity())));
            
            BigDecimal cost = position.getAvgCost().multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal unrealizedPnl = position.getMarketValue().subtract(cost);
            position.setUnrealizedPnl(unrealizedPnl);
            
            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unrealizedPnlPct = unrealizedPnl.divide(cost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                position.setUnrealizedPnlPct(unrealizedPnlPct);
            }
            
            position.setUpdatedAt(LocalDateTime.now());
            updateById(position);
        }
    }
}
