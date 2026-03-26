package com.quant.trading.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quant.trading.entity.Trade;
import com.quant.trading.mapper.TradeMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TradeService extends ServiceImpl<TradeMapper, Trade> {
    
    public List<Trade> getRecentTrades(int limit) {
        return baseMapper.findRecent(limit);
    }
    
    public List<Trade> getTradesByStockCode(String stockCode) {
        return baseMapper.findByStockCode(stockCode);
    }
    
    public Trade createTrade(String orderId, String stockCode, String stockName, String side,
                             int quantity, BigDecimal price, BigDecimal commission,
                             BigDecimal stampDuty, BigDecimal transferFee, BigDecimal slippageCost,
                             BigDecimal realizedPnl, String strategyReason) {
        Trade trade = new Trade();
        trade.setTradeId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        trade.setOrderId(orderId);
        trade.setStockCode(stockCode);
        trade.setStockName(stockName);
        trade.setSide(side);
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setAmount(price.multiply(BigDecimal.valueOf(quantity)));
        trade.setCommission(commission);
        trade.setStampDuty(stampDuty);
        trade.setTransferFee(transferFee);
        trade.setSlippageCost(slippageCost);
        trade.setRealizedPnl(realizedPnl);
        trade.setTradedAt(LocalDateTime.now());
        trade.setStrategyReason(strategyReason);
        save(trade);
        return trade;
    }
}
