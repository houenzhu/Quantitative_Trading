package com.quant.trading.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quant.trading.entity.StockPoolItem;
import com.quant.trading.mapper.StockPoolMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockPoolService extends ServiceImpl<StockPoolMapper, StockPoolItem> {
    
    public List<StockPoolItem> getActiveStocks() {
        return baseMapper.findActive();
    }
    
    public Map<String, String> getStockPoolMap() {
        Map<String, String> pool = new HashMap<>();
        List<StockPoolItem> items = getActiveStocks();
        for (StockPoolItem item : items) {
            pool.put(item.getStockCode(), item.getStockName());
        }
        return pool;
    }
    
    public boolean addStock(String stockCode, String stockName) {
        StockPoolItem existing = baseMapper.findByStockCode(stockCode);
        if (existing != null) {
            if (!existing.getIsActive()) {
                existing.setIsActive(true);
                existing.setRemovedAt(null);
                updateById(existing);
                return true;
            }
            return false;
        }
        
        StockPoolItem item = new StockPoolItem();
        item.setStockCode(stockCode);
        item.setStockName(stockName);
        item.setIsActive(true);
        item.setPriority(0);
        save(item);
        return true;
    }
    
    public boolean removeStock(String stockCode) {
        int updated = baseMapper.deactivateByStockCode(stockCode);
        return updated > 0;
    }
    
    public boolean isInPool(String stockCode) {
        return baseMapper.findByStockCode(stockCode) != null;
    }
}
