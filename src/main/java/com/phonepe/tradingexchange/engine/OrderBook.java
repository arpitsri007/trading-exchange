package com.phonepe.tradingexchange.engine;

import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.model.OrderSide;

import java.math.BigDecimal;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OrderBook implements IOrderBook {
    private final String symbol;
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;
    private final ReentrantReadWriteLock lock;
    
    public OrderBook(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new OrderException("Symbol cannot be null or empty");
        }
        
        this.symbol = symbol;
        this.lock = new ReentrantReadWriteLock();

        this.buyOrders = new PriorityQueue<>((order1, order2) -> {
            int priceComparison = order2.getPrice().compareTo(order1.getPrice());
            if (priceComparison == 0) {
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return priceComparison;
        });
        
        this.sellOrders = new PriorityQueue<>((order1, order2) -> {
            int priceComparison = order1.getPrice().compareTo(order2.getPrice());
            if (priceComparison == 0) {
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return priceComparison;
        });
    }
    
    @Override
    public void addOrder(Order order) {
        if (order == null) {
            throw new OrderException("Order cannot be null");
        }
        
        if (!order.getSymbol().equals(symbol)) {
            throw new OrderException("Order symbol does not match order book symbol");
        }
        
        lock.writeLock().lock();
        try {
            if (order.getSide() == OrderSide.BUY) {
                buyOrders.add(order);
            } else {
                sellOrders.add(order);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void removeOrder(Order order) {
        if (order == null) {
            throw new OrderException("Order cannot be null");
        }
        lock.writeLock().lock();
        try {
            if (order.getSide() == OrderSide.BUY) {
                buyOrders.remove(order);
            } else {
                sellOrders.remove(order);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public BigDecimal getBestBid() {
        lock.readLock().lock();
        try {
            Order bestBuy = buyOrders.peek();
            return bestBuy != null ? bestBuy.getPrice() : BigDecimal.ZERO;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public BigDecimal getBestAsk() {
        lock.readLock().lock();
        try {
            Order bestSell = sellOrders.peek();
            return bestSell != null ? bestSell.getPrice() : BigDecimal.ZERO;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public Order getNextBuyOrder() {
        lock.readLock().lock();
        try {
            return buyOrders.peek();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public Order getNextSellOrder() {
        lock.readLock().lock();
        try {
            return sellOrders.peek();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean hasMatchingOrders() {
        lock.readLock().lock();
        try {
            Order bestBuy = buyOrders.peek();
            Order bestSell = sellOrders.peek();
            
            return bestBuy != null && bestSell != null && 
                    bestBuy.getPrice().compareTo(bestSell.getPrice()) >= 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public int getTotalOrders() {
        lock.readLock().lock();
        try {
            return buyOrders.size() + sellOrders.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public String getSymbol() {
        return symbol;
    }
} 