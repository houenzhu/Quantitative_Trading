package com.quant.trading.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quant.trading.entity.Account;
import com.quant.trading.mapper.AccountMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService extends ServiceImpl<AccountMapper, Account> {
    
    public Account getLatestAccount() {
        return baseMapper.findLatest();
    }
    
    public List<Account> getRecentAccounts(int limit) {
        return baseMapper.findRecent(limit);
    }
    
    public Map<String, Object> getAccountInfo() {
        Account account = getLatestAccount();
        Map<String, Object> info = new HashMap<>();
        if (account != null) {
            info.put("totalEquity", account.getTotalEquity());
            info.put("cashBalance", account.getCash());
            info.put("marketValue", account.getPositionsValue());
            info.put("totalPnl", account.getTotalPnl());
            info.put("totalPnlPercent", account.getTotalPnl() != null && account.getInitialCapital() != null 
                    && account.getInitialCapital().compareTo(BigDecimal.ZERO) > 0
                    ? account.getTotalPnl().divide(account.getInitialCapital(), 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO);
            info.put("unrealizedPnl", account.getUnrealizedPnl());
            info.put("realizedPnl", account.getRealizedPnl());
            info.put("dailyPnl", account.getDailyPnl());
            info.put("tradeCount", account.getTradeCount());
            info.put("winCount", account.getWinCount());
            info.put("lossCount", account.getLossCount());
        }
        return info;
    }
    
    public Account initAccount(BigDecimal initialCapital) {
        Account account = new Account();
        account.setTimestamp(LocalDateTime.now());
        account.setInitialCapital(initialCapital);
        account.setCurrentCapital(initialCapital);
        account.setTotalEquity(initialCapital);
        account.setCash(initialCapital);
        account.setPositionsValue(BigDecimal.ZERO);
        account.setTotalPnl(BigDecimal.ZERO);
        account.setRealizedPnl(BigDecimal.ZERO);
        account.setUnrealizedPnl(BigDecimal.ZERO);
        account.setDailyPnl(BigDecimal.ZERO);
        account.setDailyLoss(BigDecimal.ZERO);
        account.setTradeCount(0);
        account.setWinCount(0);
        account.setLossCount(0);
        save(account);
        return account;
    }
    
    public void updateAccount(Account account) {
        account.setTimestamp(LocalDateTime.now());
        updateById(account);
    }
}
