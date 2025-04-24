package com.phonepe.tradingexchange.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages locks for orders to ensure thread safety during operations.
 * This class provides methods for acquiring and releasing locks for individual or multiple orders.
 */
public class OrderLockManager {
    private final ConcurrentHashMap<String, ReentrantLock> orderLocks = new ConcurrentHashMap<>();
    
    private static OrderLockManager INSTANCE;
    
    private OrderLockManager() {}
    
    public static OrderLockManager getInstance() {
        if (INSTANCE == null) {
            synchronized (OrderLockManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new OrderLockManager();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Gets or creates the lock for an order ID.
     * 
     * @param orderId Order ID to get/create lock for
     * @return ReentrantLock for the specified order ID
     */
    public ReentrantLock getOrderLock(String orderId) {
        return orderLocks.computeIfAbsent(orderId, id -> new ReentrantLock());
    }
    
    /**
     * Acquires locks for two orders in a consistent order to prevent deadlock.
     * 
     * @param firstOrderId First order ID
     * @param secondOrderId Second order ID
     * @return Array of locks in the order they were acquired
     */
    public ReentrantLock[] acquireOrderLocks(String firstOrderId, String secondOrderId) {
        ReentrantLock firstLock = getOrderLock(firstOrderId);
        ReentrantLock secondLock = getOrderLock(secondOrderId);
        
        // Acquire locks in a consistent order to prevent deadlock
        if (firstOrderId.compareTo(secondOrderId) < 0) {
            firstLock.lock();
            secondLock.lock();
        } else {
            secondLock.lock();
            firstLock.lock();
        }
        
        return new ReentrantLock[] { firstLock, secondLock };
    }
    
    /**
     * Releases the provided locks if they are held by the current thread.
     * 
     * @param locks Locks to release
     */
    public void releaseLocks(ReentrantLock... locks) {
        for (ReentrantLock lock : locks) {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    /**
     * Acquires a lock for a single order.
     * 
     * @param orderId Order ID to lock
     * @return The acquired lock
     */
    public ReentrantLock acquireLock(String orderId) {
        ReentrantLock lock = getOrderLock(orderId);
        lock.lock();
        return lock;
    }
    
    /**
     * Clears all locks - should only be used for testing/cleanup.
     */
    public void clearLocks() {
        orderLocks.clear();
    }
} 