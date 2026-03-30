package com.quant.trading.scheduler;

import com.quant.trading.entity.*;
import com.quant.trading.mapper.SignalMapper;
import com.quant.trading.mapper.StrategyMapper;
import com.quant.trading.service.*;
import com.quant.trading.strategy.CompositeStrategy;
import com.quant.trading.strategy.EnhancedMomentumStrategy;
import com.quant.trading.strategy.MomentumStrategy;
import com.quant.trading.strategy.VWAPStrategy;
import com.quant.trading.trading.TradingHours;
import com.quant.trading.websocket.MarketDataBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class RealtimeStrategyExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(RealtimeStrategyExecutor.class);
    
    @Autowired
    private MomentumStrategy momentumStrategy;
    
    @Autowired
    private EnhancedMomentumStrategy enhancedMomentumStrategy;
    
    @Autowired
    private VWAPStrategy vwapStrategy;
    
    @Autowired
    private CompositeStrategy compositeStrategy;
    
    @Autowired
    private StrategyMapper strategyMapper;
    
    @Autowired
    private SignalMapper signalMapper;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PositionService positionService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private MarketDataBroadcaster broadcaster;
    
    @Autowired
    private TradingHours tradingHours;
    
    private final Map<String, List<TickData>> tickHistoryMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastSignalTimeMap = new ConcurrentHashMap<>();
    private final Map<String, String> lastSignalTypeMap = new ConcurrentHashMap<>();
    
    private static final int SIGNAL_COOLDOWN_SECONDS = 60;
    private static final int MAX_HISTORY_SIZE = 500;
    
    private int tickCount = 0;
    
    public void onTickData(TickData tick, Map<String, String> stockPool) {
        if (!tradingHours.isTradingTime(LocalDateTime.now())) {
            return;
        }
        
        tickCount++;
        
        String stockCode = tick.getCode();
        
        tickHistoryMap.computeIfAbsent(stockCode, k -> new CopyOnWriteArrayList<>());
        List<TickData> history = tickHistoryMap.get(stockCode);
        history.add(tick);
        
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
        
        List<Strategy> activeStrategies = strategyMapper.findActiveStrategies();
        
        if (tickCount % 20 == 0) {
            logger.info("实时策略检查: 股票={}, 历史数据={}条, 活跃策略={}个, 价格={}", 
                    stockCode, history.size(), activeStrategies.size(), tick.getPrice());
        }
        
        for (Strategy strategy : activeStrategies) {
            if (!Boolean.TRUE.equals(strategy.getAutoExecute())) {
                logger.debug("策略{}未开启自动执行，跳过", strategy.getName());
                continue;
            }
            
            String strategyType = strategy.getStrategyType();
            
            if ("MOMENTUM".equals(strategyType) || "VWAP".equals(strategyType) || "COMPOSITE".equals(strategyType)) {
                try {
                    Signal signal = analyzeRealtime(strategy, stockCode, stockPool.get(stockCode), tick, history);
                    
                    if (signal != null) {
                        processSignal(strategy, signal, tick, stockPool);
                    }
                } catch (Exception e) {
                    logger.error("实时策略执行失败: {} - {}", stockCode, e.getMessage(), e);
                }
            }
        }
    }
    
    private Signal analyzeRealtime(Strategy strategy, String stockCode, String stockName, 
                                    TickData currentTick, List<TickData> history) {
        String strategyType = strategy.getStrategyType();
        String parametersJson = strategy.getParameters();
        Signal signal = null;
        
        switch (strategyType) {
            case "MOMENTUM":
                signal = momentumStrategy.analyze(stockCode, stockName, currentTick, history, parametersJson);
                break;
            case "ENHANCED_MOMENTUM":
                signal = enhancedMomentumStrategy.analyze(stockCode, stockName, currentTick, history, parametersJson);
                break;
            case "VWAP":
                signal = vwapStrategy.analyze(stockCode, stockName, currentTick, history, parametersJson);
                break;
            case "COMPOSITE":
                signal = compositeStrategy.analyze(stockCode, stockName, currentTick, history, parametersJson);
                break;
            default:
                break;
        }
        
        return signal;
    }
    
    private void processSignal(Strategy strategy, Signal signal, TickData tick, Map<String, String> stockPool) {
        String stockCode = signal.getStockCode();
        String signalAction = signal.getAction().toUpperCase();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSignalTime = lastSignalTimeMap.get(stockCode);
        String lastSignalType = lastSignalTypeMap.get(stockCode);
        
        if (lastSignalTime != null && lastSignalType != null && lastSignalType.equals(signalAction)) {
            long secondsSinceLastSignal = java.time.Duration.between(lastSignalTime, now).getSeconds();
            if (secondsSinceLastSignal < SIGNAL_COOLDOWN_SECONDS) {
                logger.debug("信号冷却中，跳过: {} - {}秒前已发出{}信号", stockCode, secondsSinceLastSignal, signalAction);
                return;
            }
        }
        
        SignalEntity signalEntity = createSignalEntity(strategy, signal, tick);
        signalMapper.insert(signalEntity);
        
        lastSignalTimeMap.put(stockCode, now);
        lastSignalTypeMap.put(stockCode, signalAction);
        
        broadcaster.broadcastSignal(signalEntity);
        
        logger.info("实时策略信号: {} {} @ {} - {}", signalAction, stockCode, tick.getPrice(), signal.getReason());
        
        executeTrade(strategy, signalEntity, tick, stockPool);
    }
    
    private SignalEntity createSignalEntity(Strategy strategy, Signal signal, TickData tick) {
        SignalEntity entity = new SignalEntity();
        entity.setUserId(strategy.getUserId());
        entity.setStrategyId(strategy.getId());
        entity.setStockCode(signal.getStockCode());
        entity.setStockName(signal.getStockName());
        entity.setSignalType(signal.getAction().toUpperCase());
        entity.setSignalStrength(signal.getStrength());
        entity.setPrice(tick.getPrice());
        entity.setReason(signal.getReason());
        entity.setStatus("pending");
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
    
    @Transactional
    public void executeTrade(Strategy strategy, SignalEntity signal, TickData tick, Map<String, String> stockPool) {
        Long userId = strategy.getUserId();
        String stockCode = signal.getStockCode();
        String stockName = stockPool.getOrDefault(stockCode, signal.getStockName());
        String side = signal.getSignalType();
        BigDecimal price = tick.getPrice();
        
        int tradeSize = strategy.getTradeSize() != null ? strategy.getTradeSize() : 100;
        BigDecimal maxPositionPct = strategy.getMaxPositionPct() != null ? strategy.getMaxPositionPct() : new BigDecimal("0.1");
        
        Account account = accountService.getLatestAccountByUserId(userId);
        if (account == null) {
            logger.warn("用户{}账户不存在，无法执行交易", userId);
            return;
        }
        
        if ("BUY".equals(side)) {
            executeBuy(strategy, signal, account, stockCode, stockName, price, tradeSize, maxPositionPct);
        } else if ("SELL".equals(side)) {
            executeSell(strategy, signal, account, stockCode, stockName, price, tradeSize);
        }
    }
    
    private void executeBuy(Strategy strategy, SignalEntity signal, Account account, 
                            String stockCode, String stockName, BigDecimal price, 
                            int tradeSize, BigDecimal maxPositionPct) {
        Long userId = strategy.getUserId();
        
        BigDecimal maxPositionValue = account.getTotalEquity().multiply(maxPositionPct);
        int maxShares = maxPositionValue.divide(price, 0, RoundingMode.DOWN).intValue();
        int quantity = Math.min(tradeSize, maxShares);
        
        if (quantity <= 0) {
            logger.warn("可用资金不足，无法买入 {}", stockCode);
            return;
        }
        
        BigDecimal requiredCash = price.multiply(BigDecimal.valueOf(quantity));
        if (account.getCash().compareTo(requiredCash) < 0) {
            quantity = account.getCash().divide(price, 0, RoundingMode.DOWN).intValue();
            if (quantity <= 0) {
                logger.warn("现金不足，无法买入 {}", stockCode);
                return;
            }
            requiredCash = price.multiply(BigDecimal.valueOf(quantity));
        }
        
        Order order = orderService.createOrderForUser(userId, stockCode, stockName, "BUY", "MARKET", price, quantity, "实时策略自动执行");
        order.setStatus("filled");
        order.setFilledQuantity(quantity);
        order.setAvgFillPrice(price);
        order.setFilledAt(LocalDateTime.now());
        order.setCommission(requiredCash.multiply(new BigDecimal("0.0003")));
        orderService.updateById(order);
        
        Position position = positionService.openPositionForUser(userId, stockCode, stockName, quantity, price);
        
        BigDecimal commission = order.getCommission();
        BigDecimal newPositionValue = price.multiply(BigDecimal.valueOf(quantity));
        
        BigDecimal newCash = account.getCash().subtract(requiredCash).subtract(commission);
        BigDecimal newPositionsValue = account.getPositionsValue().add(newPositionValue);
        BigDecimal newTotalEquity = newCash.add(newPositionsValue);
        
        account.setCash(newCash);
        account.setPositionsValue(newPositionsValue);
        account.setTotalEquity(newTotalEquity);
        account.setTradeCount(account.getTradeCount() + 1);
        accountService.updateAccount(account);
        
        signal.setStatus("executed");
        signal.setExecutedAt(LocalDateTime.now());
        signal.setExecutedPrice(price);
        signal.setOrderId(order.getOrderId());
        signalMapper.updateById(signal);
        
        broadcaster.broadcastOrderUpdateForUser(userId, order);
        broadcaster.broadcastPositionUpdateForUser(userId, position);
        broadcaster.broadcastAccountUpdateForUser(userId, account);
        
        strategyMapper.incrementExecutedSignals(strategy.getId());
        
        logger.info("实时买入成功: {} {}股 @ {}，订单ID: {}, 用户ID: {}", stockCode, quantity, price, order.getOrderId(), userId);
    }
    
    private void executeSell(Strategy strategy, SignalEntity signal, Account account,
                             String stockCode, String stockName, BigDecimal price, int tradeSize) {
        Long userId = strategy.getUserId();
        
        Position position = positionService.getPositionByUserIdAndStockCode(userId, stockCode);
        
        if (position == null || position.getQuantity() <= 0) {
            logger.warn("没有持仓，无法卖出 {}", stockCode);
            return;
        }
        
        int quantity = Math.min(tradeSize, position.getQuantity());
        
        Order order = orderService.createOrderForUser(userId, stockCode, stockName, "SELL", "MARKET", price, quantity, "实时策略自动执行");
        order.setStatus("filled");
        order.setFilledQuantity(quantity);
        order.setAvgFillPrice(price);
        order.setFilledAt(LocalDateTime.now());
        BigDecimal tradeValue = price.multiply(BigDecimal.valueOf(quantity));
        order.setCommission(tradeValue.multiply(new BigDecimal("0.0003")));
        order.setSlippage(BigDecimal.ZERO);
        orderService.updateById(order);
        
        BigDecimal sellAmount = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal realizedPnl = sellAmount.subtract(position.getAvgCost().multiply(BigDecimal.valueOf(quantity)));
        BigDecimal soldPositionValue = position.getAvgCost().multiply(BigDecimal.valueOf(quantity));
        
        Position updatedPosition = positionService.closePositionForUser(userId, stockCode, quantity, price);
        
        BigDecimal commission = order.getCommission();
        
        BigDecimal newCash = account.getCash().add(sellAmount).subtract(commission);
        BigDecimal newPositionsValue = account.getPositionsValue().subtract(soldPositionValue);
        if (newPositionsValue.compareTo(BigDecimal.ZERO) < 0) {
            newPositionsValue = BigDecimal.ZERO;
        }
        BigDecimal newTotalEquity = newCash.add(newPositionsValue);
        
        account.setCash(newCash);
        account.setPositionsValue(newPositionsValue);
        account.setTotalEquity(newTotalEquity);
        account.setRealizedPnl(account.getRealizedPnl().add(realizedPnl));
        account.setTotalPnl(account.getTotalPnl().add(realizedPnl));
        account.setTradeCount(account.getTradeCount() + 1);
        
        if (realizedPnl.compareTo(BigDecimal.ZERO) > 0) {
            account.setWinCount(account.getWinCount() + 1);
        } else {
            account.setLossCount(account.getLossCount() + 1);
        }
        
        accountService.updateAccount(account);
        
        signal.setStatus("executed");
        signal.setExecutedAt(LocalDateTime.now());
        signal.setExecutedPrice(price);
        signal.setOrderId(order.getOrderId());
        signal.setPnl(realizedPnl);
        signalMapper.updateById(signal);
        
        broadcaster.broadcastOrderUpdateForUser(userId, order);
        if (updatedPosition != null) {
            broadcaster.broadcastPositionUpdateForUser(userId, updatedPosition);
        }
        broadcaster.broadcastAccountUpdateForUser(userId, account);
        
        strategyMapper.incrementExecutedSignals(strategy.getId());
        
        logger.info("实时卖出成功: {} {}股 @ {}，订单ID: {}，盈亏: {}, 用户ID: {}", stockCode, quantity, price, order.getOrderId(), realizedPnl, userId);
    }
    
    public void clearHistory(String stockCode) {
        tickHistoryMap.remove(stockCode);
        lastSignalTimeMap.remove(stockCode);
        lastSignalTypeMap.remove(stockCode);
    }
    
    public void clearAllHistory() {
        tickHistoryMap.clear();
        lastSignalTimeMap.clear();
        lastSignalTypeMap.clear();
    }
}
