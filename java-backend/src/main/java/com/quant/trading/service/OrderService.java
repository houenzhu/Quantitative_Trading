package com.quant.trading.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quant.trading.entity.Order;
import com.quant.trading.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {
    
    public Order getOrderByOrderId(String orderId) {
        return baseMapper.findByOrderId(orderId);
    }
    
    public List<Order> getRecentOrders(int limit) {
        return baseMapper.findRecent(limit);
    }
    
    public List<Order> getActiveOrders() {
        return baseMapper.findActiveOrders();
    }
    
    public List<Order> getOrdersByStockCode(String stockCode) {
        return baseMapper.findByStockCode(stockCode);
    }
    
    public Order createOrder(String stockCode, String stockName, String side, String orderType, 
                             java.math.BigDecimal price, int quantity, String reason) {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setStockCode(stockCode);
        order.setStockName(stockName);
        order.setSide(side);
        order.setOrderType(orderType);
        order.setPrice(price);
        order.setQuantity(quantity);
        order.setFilledQuantity(0);
        order.setAvgFillPrice(java.math.BigDecimal.ZERO);
        order.setStatus("pending");
        order.setReason(reason);
        order.setCommission(java.math.BigDecimal.ZERO);
        order.setSlippage(java.math.BigDecimal.ZERO);
        save(order);
        return order;
    }
    
    public void updateOrderStatus(String orderId, String status) {
        Order order = getOrderByOrderId(orderId);
        if (order != null) {
            order.setStatus(status);
            if ("submitted".equals(status)) {
                order.setSubmittedAt(LocalDateTime.now());
            } else if ("filled".equals(status)) {
                order.setFilledAt(LocalDateTime.now());
            } else if ("cancelled".equals(status)) {
                order.setCancelledAt(LocalDateTime.now());
            }
            updateById(order);
        }
    }
    
    public void fillOrder(String orderId, int filledQuantity, java.math.BigDecimal avgFillPrice, 
                          java.math.BigDecimal commission, java.math.BigDecimal slippage) {
        Order order = getOrderByOrderId(orderId);
        if (order != null) {
            order.setFilledQuantity(filledQuantity);
            order.setAvgFillPrice(avgFillPrice);
            order.setCommission(commission);
            order.setSlippage(slippage);
            order.setStatus("filled");
            order.setFilledAt(LocalDateTime.now());
            updateById(order);
        }
    }
}
