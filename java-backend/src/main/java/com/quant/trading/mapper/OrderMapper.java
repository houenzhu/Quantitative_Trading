package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
    @Select("SELECT * FROM orders WHERE order_id = #{orderId}")
    Order findByOrderId(String orderId);
    
    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Order> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Select("SELECT * FROM orders ORDER BY created_at DESC LIMIT #{limit}")
    List<Order> findRecent(int limit);
    
    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND status IN ('pending', 'submitted', 'partial') ORDER BY created_at DESC")
    List<Order> findActiveOrdersByUserId(Long userId);
    
    @Select("SELECT * FROM orders WHERE status IN ('pending', 'submitted', 'partial') ORDER BY created_at DESC")
    List<Order> findActiveOrders();
    
    @Select("SELECT * FROM orders WHERE stock_code = #{stockCode} ORDER BY created_at DESC")
    List<Order> findByStockCode(String stockCode);
    
    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND stock_code = #{stockCode} ORDER BY created_at DESC")
    List<Order> findByUserIdAndStockCode(@Param("userId") Long userId, @Param("stockCode") String stockCode);
}
