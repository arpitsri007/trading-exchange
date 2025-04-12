package com.phonepe.tradingexchange.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
public class Trade {
    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String symbol;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private final LocalDateTime executedAt;
    
    private static final AtomicLong TRADE_SEQUENCE = new AtomicLong(1);
    
    public static Trade createTrade(Order buyOrder, Order sellOrder, BigDecimal executedPrice, 
                                    BigDecimal executedQuantity) {
        if (buyOrder.getSide() != OrderSide.BUY || sellOrder.getSide() != OrderSide.SELL) {
            throw new IllegalArgumentException("Invalid order sides for trade execution");
        }
        
        String tradeId = generateTradeId();
        
        return Trade.builder()
                .tradeId(tradeId)
                .buyOrderId(buyOrder.getOrderId())
                .sellOrderId(sellOrder.getOrderId())
                .symbol(buyOrder.getSymbol())
                .price(executedPrice)
                .quantity(executedQuantity)
                .executedAt(LocalDateTime.now())
                .build();
    }
    
    private static String generateTradeId() {
        return "TRD-" + TRADE_SEQUENCE.getAndIncrement();
    }
} 