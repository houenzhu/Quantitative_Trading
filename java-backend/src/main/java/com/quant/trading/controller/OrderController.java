package com.quant.trading.controller;

import com.quant.trading.common.Result;
import com.quant.trading.entity.Order;
import com.quant.trading.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping("/recent")
    public Result<List<Order>> getRecentOrders(@RequestParam(defaultValue = "100") int limit) {
        List<Order> orders = orderService.getRecentOrders(limit);
        return Result.success(orders);
    }
    
    @GetMapping("/{orderId}")
    public Result<Order> getOrderByOrderId(@PathVariable String orderId) {
        Order order = orderService.getOrderByOrderId(orderId);
        return Result.success(order);
    }
    
    @GetMapping("/stock/{stockCode}")
    public Result<List<Order>> getOrdersByStockCode(@PathVariable String stockCode) {
        List<Order> orders = orderService.getOrdersByStockCode(stockCode);
        return Result.success(orders);
    }
    
    @PostMapping("/create")
    public Result<Order> createOrder(
            @RequestParam String stockCode,
            @RequestParam String stockName,
            @RequestParam String side,
            @RequestParam String orderType,
            @RequestParam BigDecimal price,
            @RequestParam int quantity,
            @RequestParam(required = false) String reason) {
        Order order = orderService.createOrder(stockCode, stockName, side, orderType, price, quantity, reason);
        return Result.success(order);
    }
    
    @PostMapping("/status")
    public Result<Void> updateOrderStatus(
            @RequestParam String orderId,
            @RequestParam String status) {
        orderService.updateOrderStatus(orderId, status);
        return Result.success();
    }
}
