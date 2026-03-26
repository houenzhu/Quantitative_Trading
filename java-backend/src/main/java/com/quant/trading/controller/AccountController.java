package com.quant.trading.controller;

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
        Account account = accountService.getLatestAccount();
        return Result.success(account);
    }
    
    @GetMapping("/recent")
    public Result<List<Account>> getRecentAccounts(@RequestParam(defaultValue = "10") int limit) {
        List<Account> accounts = accountService.getRecentAccounts(limit);
        return Result.success(accounts);
    }
    
    @PostMapping("/init")
    public Result<Account> initAccount(@RequestParam(defaultValue = "1000000") BigDecimal initialCapital) {
        Account account = accountService.initAccount(initialCapital);
        return Result.success(account);
    }
}
