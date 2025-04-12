package com.phonepe.tradingexchange.engine;

import com.phonepe.tradingexchange.model.Order;

import java.math.BigDecimal;

public interface IOrderBook {

    void addOrder(Order order);
    
    void removeOrder(Order order);
    
    BigDecimal getBestBid();
    
    BigDecimal getBestAsk();

    Order getNextBuyOrder();

    Order getNextSellOrder();
    
    boolean hasMatchingOrders();
    
    int getTotalOrders();
    
    String getSymbol();
} 