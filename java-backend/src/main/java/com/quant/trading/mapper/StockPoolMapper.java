package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.StockPoolItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface StockPoolMapper extends BaseMapper<StockPoolItem> {
    
    @Select("SELECT * FROM stock_pool WHERE is_active = 1 ORDER BY priority DESC")
    List<StockPoolItem> findActive();
    
    @Select("SELECT * FROM stock_pool WHERE user_id = #{userId} AND is_active = 1 ORDER BY priority DESC")
    List<StockPoolItem> findActiveByUserId(Long userId);
    
    @Select("SELECT * FROM stock_pool WHERE stock_code = #{stockCode} AND is_active = 1")
    StockPoolItem findByStockCode(String stockCode);
    
    @Select("SELECT * FROM stock_pool WHERE user_id = #{userId} AND stock_code = #{stockCode} AND is_active = 1")
    StockPoolItem findByUserIdAndStockCode(Long userId, String stockCode);
    
    @Update("UPDATE stock_pool SET is_active = 0, removed_at = NOW() WHERE stock_code = #{stockCode} AND is_active = 1")
    int deactivateByStockCode(String stockCode);
    
    @Update("UPDATE stock_pool SET is_active = 0, removed_at = NOW() WHERE user_id = #{userId} AND stock_code = #{stockCode} AND is_active = 1")
    int deactivateByUserIdAndStockCode(Long userId, String stockCode);
}
