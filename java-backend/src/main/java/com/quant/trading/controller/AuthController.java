package com.quant.trading.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.quant.trading.common.Result;
import com.quant.trading.entity.User;
import com.quant.trading.service.AccountService;
import com.quant.trading.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AccountService accountService;
    
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        if (user.getStatus() != 1) {
            return Result.error("账号已被禁用");
        }
        
        if (!BCrypt.checkpw(password, user.getPassword())) {
            return Result.error("密码错误");
        }
        
        StpUtil.login(user.getId());
        
        userService.updateLastLogin(user.getId());
        
        String token = StpUtil.getTokenValue();
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", buildUserInfo(user));
        
        return Result.success(data);
    }
    
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String nickname = params.get("nickname");
        
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }
        
        if (userService.getByUsername(username) != null) {
            return Result.error("用户名已存在");
        }
        
        String hashedPassword = BCrypt.hashpw(password);
        
        User user = userService.register(username, hashedPassword, nickname);
        
        accountService.initAccount(user.getId(), user.getInitialCapital() != null ? user.getInitialCapital() : new BigDecimal("1000000"));
        
        StpUtil.login(user.getId());
        
        String token = StpUtil.getTokenValue();
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", buildUserInfo(user));
        
        return Result.success(data);
    }
    
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
    
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo() {
        if (!StpUtil.isLogin()) {
            return Result.error("未登录");
        }
        
        User user = userService.getCurrentUser();
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        return Result.success(buildUserInfo(user));
    }
    
    @GetMapping("/check")
    public Result<Map<String, Object>> checkLogin() {
        Map<String, Object> data = new HashMap<>();
        data.put("isLogin", StpUtil.isLogin());
        if (StpUtil.isLogin()) {
            User user = userService.getCurrentUser();
            data.put("userInfo", buildUserInfo(user));
        }
        return Result.success(data);
    }
    
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("avatar", user.getAvatar());
        info.put("initialCapital", user.getInitialCapital());
        info.put("currentCapital", user.getCurrentCapital());
        
        Map<String, Object> accountInfo = accountService.getAccountInfoByUserId(user.getId());
        if (accountInfo != null && !accountInfo.isEmpty()) {
            info.put("totalEquity", accountInfo.get("totalEquity"));
            info.put("cashBalance", accountInfo.get("cashBalance"));
            info.put("marketValue", accountInfo.get("marketValue"));
            info.put("totalPnl", accountInfo.get("totalPnl"));
            info.put("totalPnlPercent", accountInfo.get("totalPnlPercent"));
            info.put("unrealizedPnl", accountInfo.get("unrealizedPnl"));
            info.put("realizedPnl", accountInfo.get("realizedPnl"));
            info.put("dailyPnl", accountInfo.get("dailyPnl"));
            info.put("tradeCount", accountInfo.get("tradeCount"));
            info.put("winCount", accountInfo.get("winCount"));
            info.put("lossCount", accountInfo.get("lossCount"));
        }
        
        return info;
    }
}
