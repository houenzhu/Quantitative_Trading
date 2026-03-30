package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
    
    @Select("SELECT * FROM accounts ORDER BY timestamp DESC LIMIT 1")
    Account findLatest();
    
    @Select("SELECT * FROM accounts WHERE user_id = #{userId} ORDER BY timestamp DESC LIMIT 1")
    Account findLatestByUserId(Long userId);
    
    @Select("SELECT * FROM accounts ORDER BY timestamp DESC LIMIT #{limit}")
    List<Account> findRecent(int limit);
    
    @Select("SELECT * FROM accounts WHERE user_id = #{userId} ORDER BY timestamp DESC LIMIT #{limit}")
    List<Account> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
