package com.phonepe.tradingexchange.engine;

import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.repository.OrderRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderExpiryManager {
    private final MatchingEngine matchingEngine;
    private final OrderRepository orderRepository;
    private final ScheduledExecutorService scheduler;
    
    private static OrderExpiryManager INSTANCE;
    
    private OrderExpiryManager() {
        this.matchingEngine = MatchingEngine.getInstance();
        this.orderRepository = OrderRepository.getInstance();
        this.scheduler = Executors.newScheduledThreadPool(1);
        startExpiryCheck();
        
        // Add shutdown hook to ensure proper cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    public static OrderExpiryManager getInstance() {
        if (INSTANCE == null) {
            synchronized (OrderExpiryManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new OrderExpiryManager();
                }
            }
        }
        return INSTANCE;
    }
    
    private void startExpiryCheck() {
        scheduler.scheduleAtFixedRate(this::checkExpiredOrders, 1, 1, TimeUnit.SECONDS);
    }
    
    private void checkExpiredOrders() {
        try {
            orderRepository.findAll().stream()
                .filter(Order::isActive)
                .filter(Order::isExpired)
                .forEach(this::cancelExpiredOrder);
        } catch (Exception e) {
            // Log error but don't stop the scheduler
            System.err.println("Error checking expired orders: " + e.getMessage());
        }
    }
    
    private void cancelExpiredOrder(Order order) {
        try {
            matchingEngine.cancelOrder(order.getOrderId());
        } catch (OrderException e) {
            System.err.println("Error cancelling expired order " + order.getOrderId() + ": " + e.getMessage());
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 