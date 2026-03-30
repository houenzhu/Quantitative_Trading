package com.quant.trading.scheduler;

import com.quant.trading.entity.*;
import com.quant.trading.fetcher.HistoryDataFetcher;
import com.quant.trading.mapper.KlineDataMapper;
import com.quant.trading.mapper.SignalMapper;
import com.quant.trading.mapper.StrategyMapper;
import com.quant.trading.service.*;
import com.quant.trading.trading.TradingHours;
import com.quant.trading.websocket.MarketDataBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
public class StrategyScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(StrategyScheduler.class);
    
    @Autowired
    private StrategyEngineService strategyEngineService;
    
    @Autowired
    private StockPoolService stockPoolService;
    
    @Autowired
    private HistoryDataFetcher historyDataFetcher;
    
    @Autowired
    private KlineDataMapper klineDataMapper;
    
    @Autowired
    private StrategyMapper strategyMapper;
    
    @Autowired
    private SignalMapper signalMapper;
    
    @Autowired
    private MarketDataBroadcaster broadcaster;
    
    @Autowired
    private TradingHours tradingHours;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PositionService positionService;
    
    @Autowired
    private AccountService accountService;
    
    private LocalDate lastKlineUpdateDate = null;
    private LocalDate lastStrategyExecuteDate = null;
    
    @Scheduled(fixedRate = 60000)
    public void checkAndExecuteStrategies() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        if (!tradingHours.isTradingDay(today)) {
            return;
        }
        
        if (lastStrategyExecuteDate != null && lastStrategyExecuteDate.equals(today)) {
            return;
        }
        
        LocalTime time = now.toLocalTime();
        boolean isAfterMarketOpen = time.isAfter(LocalTime.of(9, 35));
        
        if (isAfterMarketOpen) {
            logger.info("开始执行策略分析...");
            executeStrategies();
            lastStrategyExecuteDate = today;
        }
    }
    
    @Scheduled(fixedRate = 300000)
    public void updateKlineData() {
        LocalDate today = LocalDate.now();
        
        if (!tradingHours.isTradingDay(today)) {
            return;
        }
        
        if (lastKlineUpdateDate != null && lastKlineUpdateDate.equals(today)) {
            return;
        }
        
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(9, 30))) {
            return;
        }
        
        logger.info("开始更新K线数据...");
        
        Map<String, String> stockPool = stockPoolService.getStockPoolMap();
        int updatedCount = 0;
        
        for (String stockCode : stockPool.keySet()) {
            try {
                List<TickData> klines = historyDataFetcher.fetchHistoryKline(stockCode, 60);
                
                for (TickData tick : klines) {
                    KlineData klineData = new KlineData();
                    klineData.setStockCode(stockCode);
                    klineData.setPeriod("1d");
                    klineData.setOpenPrice(tick.getOpen());
                    klineData.setHighPrice(tick.getHigh());
                    klineData.setLowPrice(tick.getLow());
                    klineData.setClosePrice(tick.getPrice());
                    klineData.setVolume(tick.getVolume() != null ? tick.getVolume().longValue() : 0L);
                    klineData.setAmount(tick.getAmount());
                    klineData.setTradeTime(tick.getTime());
                    
                    try {
                        klineDataMapper.insertOrUpdate(klineData);
                        updatedCount++;
                    } catch (Exception e) {
                        logger.debug("K线数据已存在: {} - {}", stockCode, tick.getTime());
                    }
                }
                
                Thread.sleep(200);
            } catch (Exception e) {
                logger.error("更新K线数据失败: {}", stockCode, e);
            }
        }
        
        lastKlineUpdateDate = today;
        logger.info("K线数据更新完成，共更新{}条记录", updatedCount);
    }
    
    public void executeStrategies() {
        Map<String, String> stockPool = stockPoolService.getStockPoolMap();
        
        if (stockPool.isEmpty()) {
            logger.warn("股票池为空，无法执行策略");
            return;
        }
        
        List<SignalEntity> signals = strategyEngineService.executeAllActiveStrategies(stockPool);
        
        if (!signals.isEmpty()) {
            logger.info("策略执行完成，生成{}个信号", signals.size());
            
            for (SignalEntity signal : signals) {
                broadcaster.broadcastSignal(signal);
                
                Strategy strategy = strategyMapper.selectById(signal.getStrategyId());
                if (strategy != null && Boolean.TRUE.equals(strategy.getAutoExecute())) {
                    logger.info("策略{}设置了自动执行，开始自动下单: {} {} @ {}", 
                            strategy.getName(), signal.getSignalType(), signal.getStockCode(), signal.getPrice());
                    
                    try {
                        executeSignal(strategy, signal, stockPool);
                    } catch (Exception e) {
                        logger.error("自动执行信号失败: {}", e.getMessage(), e);
                    }
                }
            }
        }
    }
    
    @Transactional
    public void executeSignal(Strategy strategy, SignalEntity signal, Map<String, String> stockPool) {
        Long userId = strategy.getUserId();
        String stockCode = signal.getStockCode();
        String stockName = stockPool.getOrDefault(stockCode, signal.getStockName());
        String side = signal.getSignalType();
        BigDecimal price = signal.getPrice();
        
        int tradeSize = strategy.getTradeSize() != null ? strategy.getTradeSize() : 100;
        BigDecimal maxPositionPct = strategy.getMaxPositionPct() != null ? strategy.getMaxPositionPct() : new BigDecimal("0.1");
        
        Account account = accountService.getLatestAccountByUserId(userId);
        if (account == null) {
            logger.warn("用户{}账户不存在，无法执行交易", userId);
            return;
        }
        
        if ("BUY".equals(side)) {
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
            
            Order order = orderService.createOrderForUser(userId, stockCode, stockName, "BUY", "LIMIT", price, quantity, "策略自动执行");
            order.setStatus("filled");
            order.setFilledQuantity(quantity);
            order.setAvgFillPrice(price);
            order.setFilledAt(LocalDateTime.now());
            order.setCommission(requiredCash.multiply(new BigDecimal("0.0003")));
            orderService.updateById(order);
            
            positionService.openPositionForUser(userId, stockCode, stockName, quantity, price);
            
            account.setCash(account.getCash().subtract(requiredCash).subtract(order.getCommission()));
            account.setTradeCount(account.getTradeCount() + 1);
            accountService.updateAccount(account);
            
            signal.setStatus("executed");
            signalMapper.updateById(signal);
            
            broadcaster.broadcastOrderUpdate(order);
            broadcaster.broadcastAccountUpdate(account);
            
            logger.info("自动买入成功: {} {}股 @ {}，订单ID: {}", stockCode, quantity, price, order.getOrderId());
            
        } else if ("SELL".equals(side)) {
            Position position = positionService.getPositionByUserIdAndStockCode(userId, stockCode);
            
            if (position == null || position.getQuantity() <= 0) {
                logger.warn("没有持仓，无法卖出 {}", stockCode);
                return;
            }
            
            int quantity = Math.min(tradeSize, position.getQuantity());
            
            Order order = orderService.createOrderForUser(userId, stockCode, stockName, "SELL", "LIMIT", price, quantity, "策略自动执行");
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
            
            positionService.closePositionForUser(userId, stockCode, quantity, price);
            
            account.setCash(account.getCash().add(sellAmount).subtract(order.getCommission()));
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
            signalMapper.updateById(signal);
            
            broadcaster.broadcastOrderUpdate(order);
            broadcaster.broadcastAccountUpdate(account);
            
            logger.info("自动卖出成功: {} {}股 @ {}，订单ID: {}，盈亏: {}", stockCode, quantity, price, order.getOrderId(), realizedPnl);
        }
        
        strategyMapper.incrementExecutedSignals(strategy.getId());
    }
    
    @Scheduled(cron = "0 0 18 * * ?")
    public void dailyReport() {
        logger.info("=== 每日策略报告 ===");
        
        List<Strategy> activeStrategies = strategyMapper.findActiveStrategies();
        logger.info("活跃策略数量: {}", activeStrategies.size());
        
        for (Strategy strategy : activeStrategies) {
            logger.info("策略: {} - 总信号: {}, 已执行: {}, 盈亏: {}", 
                    strategy.getName(), 
                    strategy.getTotalSignals(), 
                    strategy.getExecutedSignals(),
                    strategy.getTotalPnl());
        }
    }
}
