package com.phonepe.tradingexchange.engine;

import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class OrderManager {
    private final OrderRepository orderRepository;
    private final MatchingEngine matchingEngine;
    private final ConcurrentHashMap<String, ReentrantLock> orderLocks;
    
    private static OrderManager INSTANCE;
    
    private OrderManager() {
        this.orderRepository = OrderRepository.getInstance();
        this.matchingEngine = MatchingEngine.getInstance();
        this.orderLocks = new ConcurrentHashMap<>();
    }
    
    public static OrderManager getInstance() {
        if (INSTANCE == null) {
            synchronized (OrderManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new OrderManager();
                }
            }
        }
        return INSTANCE;
    }
    
    public void placeOrder(Order order) throws OrderException {
        orderRepository.save(order);
        matchingEngine.addOrder(order);
    }
    
    public void cancelOrder(String orderId) throws OrderException {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException("Order not found"));
        
        if (!order.isActive()) {
            throw new OrderException("Cannot cancel inactive order");
        }
        
        ReentrantLock orderLock = orderLocks.computeIfAbsent(orderId, id -> new ReentrantLock());
        orderLock.lock();
        try {
            // Recheck if order is still active after acquiring lock
            if (!order.isActive()) {
                throw new OrderException("Cannot cancel inactive order");
            }
            
            order.cancel();
            orderRepository.updateOrder(order);
            matchingEngine.removeOrder(order);
        } finally {
            orderLock.unlock();
        }
    }
    
    public void modifyOrder(String orderId, BigDecimal newPrice, BigDecimal newQuantity) throws OrderException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found"));

        if (!order.isActive()) {
            throw new OrderException("Cannot modify inactive order");
        }

        ReentrantLock orderLock = orderLocks.computeIfAbsent(orderId, id -> new ReentrantLock());
        orderLock.lock();
        try {
            // Recheck if order is still active after acquiring lock
            if (!order.isActive()) {
                throw new OrderException("Cannot modify inactive order");
            }

            matchingEngine.removeOrder(order);

            if (newPrice != null) {
                order.updatePrice(newPrice);
            }
            if (newQuantity != null) {
                order.updateQuantity(newQuantity);
            }

            orderRepository.updateOrder(order);
            matchingEngine.addOrder(order);
        } finally {
            orderLock.unlock();
        }
    }
    
    public ReentrantLock getOrderLock(String orderId) {
        return orderLocks.computeIfAbsent(orderId, id -> new ReentrantLock());
    }
} 