package com.quant.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.trading.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
    @Select("SELECT * FROM orders WHERE order_id = #{orderId}")
    Order findByOrderId(String orderId);
    
    @Select("SELECT * FROM orders ORDER BY created_at DESC LIMIT #{limit}")
    List<Order> findRecent(int limit);
    
    @Select("SELECT * FROM orders WHERE stock_code = #{stockCode} ORDER BY created_at DESC")
    List<Order> findByStockCode(String stockCode);
}
