package com.quant.trading.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.quant.trading.common.Result;
import com.quant.trading.entity.Account;
import com.quant.trading.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    @GetMapping("/latest")
    public Result<Account> getLatestAccount() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            Account account = accountService.getLatestAccountByUserId(userId);
            return Result.success(account);
        }
        Account account = accountService.getLatestAccount();
        return Result.success(account);
    }
    
    @GetMapping("/recent")
    public Result<List<Account>> getRecentAccounts(@RequestParam(defaultValue = "10") int limit) {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            List<Account> accounts = accountService.getRecentAccountsByUserId(userId, limit);
            return Result.success(accounts);
        }
        List<Account> accounts = accountService.getRecentAccounts(limit);
        return Result.success(accounts);
    }
    
    @PostMapping("/init")
    public Result<Account> initAccount(@RequestParam(defaultValue = "1000000") BigDecimal initialCapital) {
        Long userId = null;
        if (StpUtil.isLogin()) {
            userId = StpUtil.getLoginIdAsLong();
        }
        Account account = accountService.initAccount(userId, initialCapital);
        return Result.success(account);
    }
}
