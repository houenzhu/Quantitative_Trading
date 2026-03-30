package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(String email);
    
    @Select("SELECT * FROM users WHERE phone = #{phone}")
    User findByPhone(String phone);
}
