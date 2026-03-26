package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.Trade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TradeMapper extends BaseMapper<Trade> {
    
    @Select("SELECT * FROM trades ORDER BY traded_at DESC LIMIT #{limit}")
    List<Trade> findRecent(int limit);
    
    @Select("SELECT * FROM trades WHERE stock_code = #{stockCode} ORDER BY traded_at DESC")
    List<Trade> findByStockCode(String stockCode);
}
