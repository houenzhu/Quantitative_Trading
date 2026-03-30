package com.quant.trading.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quant.trading.entity.User;
import com.quant.trading.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    
    public User getByUsername(String username) {
        return baseMapper.findByUsername(username);
    }
    
    public User getByEmail(String email) {
        return baseMapper.findByEmail(email);
    }
    
    public User getByPhone(String phone) {
        return baseMapper.findByPhone(phone);
    }
    
    public User getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return getById(userId);
    }
    
    public User register(String username, String password, String nickname) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname != null ? nickname : username);
        user.setStatus(1);
        user.setInitialCapital(new BigDecimal("1000000"));
        user.setCurrentCapital(new BigDecimal("1000000"));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        save(user);
        return user;
    }
    
    public void updateLastLogin(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginAt(LocalDateTime.now());
        updateById(user);
    }
    
    public void updateUserInfo(Long userId, String nickname, String email, String phone) {
        User user = new User();
        user.setId(userId);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }
}
