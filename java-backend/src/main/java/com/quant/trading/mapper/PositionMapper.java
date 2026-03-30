package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface PositionMapper extends BaseMapper<Position> {
    
    @Select("SELECT * FROM positions WHERE is_active = 1")
    List<Position> findActivePositions();
    
    @Select("SELECT * FROM positions WHERE user_id = #{userId} AND is_active = 1")
    List<Position> findActivePositionsByUserId(Long userId);
    
    @Select("SELECT * FROM positions WHERE stock_code = #{stockCode} AND is_active = 1")
    Position findByStockCode(String stockCode);
    
    @Select("SELECT * FROM positions WHERE user_id = #{userId} AND stock_code = #{stockCode} AND is_active = 1")
    Position findByUserIdAndStockCode(Long userId, String stockCode);
    
    @Update("UPDATE positions SET is_active = 0, closed_at = NOW(), updated_at = NOW() WHERE stock_code = #{stockCode} AND is_active = 1")
    int deactivateByStockCode(String stockCode);
    
    @Update("UPDATE positions SET is_active = 0, closed_at = NOW(), updated_at = NOW() WHERE user_id = #{userId} AND stock_code = #{stockCode} AND is_active = 1")
    int deactivateByUserIdAndStockCode(Long userId, String stockCode);
}
