package com.phonepe.tradingexchange.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
public class Order {
    private final String orderId;
    private final String userId;
    private final String symbol;
    private final OrderSide side;
    private BigDecimal price;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal quantity;
    private OrderStatus status;
    
    private static final AtomicLong ORDER_SEQUENCE = new AtomicLong(1);
    
    public static Order createOrder(String userId, String symbol, OrderSide side, 
                                  BigDecimal price, BigDecimal quantity) {
        LocalDateTime now = LocalDateTime.now();
        return Order.builder()
                .orderId("ORD-" + ORDER_SEQUENCE.getAndIncrement())
                .userId(userId)
                .symbol(symbol)
                .side(side)
                .price(price)
                .quantity(quantity)
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
    
    public void updateQuantity(BigDecimal newQuantity) {
        this.quantity = newQuantity;
        this.updatedAt = LocalDateTime.now();
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            this.status = OrderStatus.EXECUTED;
        }
    }
    
    public void updatePrice(BigDecimal newPrice) {
        this.price = newPrice;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return status == OrderStatus.OPEN;
    }
} 