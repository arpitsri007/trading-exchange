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
    private LocalDateTime expiryTime;
    private OrderType orderType;
    private BigDecimal stopLossPrice;
    private BigDecimal takeProfitPrice;

    // ABC - O1, [O2 , O3], O4, O5
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
                .expiryTime(now.plusMinutes(5))
                .orderType(OrderType.MARKET)
                .build();
    }
    
    public static Order createStopLossOrder(String userId, String symbol, OrderSide side,
                                          BigDecimal price, BigDecimal quantity,
                                          BigDecimal stopLossPrice) {
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
                .expiryTime(now.plusMinutes(5))
                .orderType(OrderType.STOP_LOSS)
                .stopLossPrice(stopLossPrice)
                .build();
    }

    public static Order createTakeProfitOrder(String userId, String symbol, OrderSide side,
                                            BigDecimal price, BigDecimal quantity,
                                            BigDecimal takeProfitPrice) {
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
                .expiryTime(now.plusMinutes(5))
                .orderType(OrderType.TAKE_PROFIT)
                .takeProfitPrice(takeProfitPrice)
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
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public boolean isStopLossTriggered(BigDecimal currentPrice) {
        if (orderType != OrderType.STOP_LOSS || stopLossPrice == null) {
            return false;
        }
        return side == OrderSide.BUY ? 
            currentPrice.compareTo(stopLossPrice) <= 0 : 
            currentPrice.compareTo(stopLossPrice) >= 0;
    }

    public boolean isTakeProfitTriggered(BigDecimal currentPrice) {
        if (orderType != OrderType.TAKE_PROFIT || takeProfitPrice == null) {
            return false;
        }
        return side == OrderSide.BUY ? 
            currentPrice.compareTo(takeProfitPrice) >= 0 : 
            currentPrice.compareTo(takeProfitPrice) <= 0;
    }
} 