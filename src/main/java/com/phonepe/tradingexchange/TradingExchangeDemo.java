package com.phonepe.tradingexchange;

import com.phonepe.tradingexchange.model.OrderSide;
import com.phonepe.tradingexchange.service.TradingService;

import java.math.BigDecimal;

public class TradingExchangeDemo {
    public static void main(String[] args) {
        TradingService tradingService = TradingService.getInstance();
        
        var user1 = tradingService.registerUser("abc", "abc@example.com");
        var user2 = tradingService.registerUser("xyz", "xyz@example.com");
        
        System.out.println("Registered users:");
        System.out.println("User 1: " + user1);
        System.out.println("User 2: " + user2);
        System.out.println();
        
        System.out.println("Placing orders...");
        
        var buyOrder = tradingService.placeOrder(
                user1.getUserId(),
                "AAPL",
                OrderSide.BUY,
                new BigDecimal("150.00"),
                new BigDecimal("100")
        );
        System.out.println("abc placed buy order: " + buyOrder);
        
        var sellOrder1 = tradingService.placeOrder(
                user2.getUserId(),
                "AAPL",
                OrderSide.SELL,
                new BigDecimal("151.00"),
                new BigDecimal("50")
        );
        System.out.println("xyz placed sell order: " + sellOrder1);
        
        System.out.println("\nMarket data:");
        System.out.println(tradingService.getMarketData("AAPL"));
        
        // Modify the buy order to match the sell order
        System.out.println("\nModifying buy order to match sell order...");
        tradingService.modifyOrder(buyOrder.getOrderId(), new BigDecimal("151.00"), null);
        
        System.out.println("\nMarket data after modification:");
        System.out.println(tradingService.getMarketData("AAPL"));
        
        System.out.println("\nTrades for AAPL:");
        tradingService.getSymbolTrades("AAPL").forEach(System.out::println);
        
        System.out.println("\nabc's orders:");
        tradingService.getUserOrders(user1.getUserId()).forEach(System.out::println);
        
        System.out.println("\nxyz's orders:");
        tradingService.getUserOrders(user2.getUserId()).forEach(System.out::println);
        
        System.out.println("\nabc's trades:");
        tradingService.getUserTrades(user1.getUserId()).forEach(System.out::println);
        
        System.out.println("\nxyz's trades:");
        tradingService.getUserTrades(user2.getUserId()).forEach(System.out::println);
    }
} 