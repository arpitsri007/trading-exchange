package com.phonepe.tradingexchange.engine;

import com.phonepe.tradingexchange.concurrent.OrderLockManager;
import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.model.Trade;
import com.phonepe.tradingexchange.repository.OrderRepository;
import com.phonepe.tradingexchange.repository.TradeRepository;
import com.phonepe.tradingexchange.util.ValidationUtils;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class MatchingEngine {
    private final ConcurrentHashMap<String, IOrderBook> orderBooks = new ConcurrentHashMap<>();
    private OrderRepository orderRepository;
    private TradeRepository tradeRepository;
    private final OrderLockManager lockManager = OrderLockManager.getInstance();
    
    private static MatchingEngine INSTANCE;
    
    private MatchingEngine() {}
    
    public static MatchingEngine getInstance() {
        if (INSTANCE == null) {
            synchronized (MatchingEngine.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MatchingEngine();
                }
            }
        }
        return INSTANCE;
    }

    public static void reset() {
        INSTANCE = null;
    }
    
    public void setRepositories(OrderRepository orderRepository, TradeRepository tradeRepository) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
    }
    
    public void addOrder(Order order) throws OrderException {
        IOrderBook orderBook = orderBooks.computeIfAbsent(
            order.getSymbol(), 
            symbol -> new OrderBook(symbol)
        );
        
        orderRepository.save(order);
        orderBook.addOrder(order);
        matchOrders(orderBook);
    }
    
    public void removeOrder(Order order) {
        IOrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook != null) {
            orderBook.removeOrder(order);
        }
    }
    
    private void matchOrders(IOrderBook orderBook) {
        while (orderBook.hasMatchingOrders()) {
            Order buyOrder = orderBook.getNextBuyOrder();
            Order sellOrder = orderBook.getNextSellOrder();
            
            if (buyOrder == null || sellOrder == null) break;
            
            // Get locks for both orders
            ReentrantLock[] locks = lockManager.acquireOrderLocks(buyOrder.getOrderId(), sellOrder.getOrderId());
            
            try {
                // Recheck if orders are still valid after acquiring locks
                if (!buyOrder.isActive() || !sellOrder.isActive()) {
                    continue;
                }

                // Check if stop-loss or take-profit orders are triggered
                BigDecimal currentPrice = sellOrder.getPrice();
                if (buyOrder.isStopLossTriggered(currentPrice) || buyOrder.isTakeProfitTriggered(currentPrice) ||
                    sellOrder.isStopLossTriggered(currentPrice) || sellOrder.isTakeProfitTriggered(currentPrice)) {
                    continue;
                }
                
                // Check for any stop-loss or take-profit orders that should be triggered
                ((OrderBook) orderBook).checkStopLossAndTakeProfit(currentPrice);
                
                BigDecimal executionPrice = sellOrder.getPrice();
                BigDecimal executionQuantity = buyOrder.getQuantity().min(sellOrder.getQuantity());
                
                Trade trade = Trade.createTrade(buyOrder, sellOrder, executionPrice, executionQuantity);
                tradeRepository.addTrade(trade);
                
                processOrderExecution(buyOrder, sellOrder, executionQuantity, orderBook);
            } finally {
                lockManager.releaseLocks(locks);
            }
        }
    }
    
    private void processOrderExecution(Order buyOrder, Order sellOrder, BigDecimal executionQuantity, IOrderBook orderBook) {
        BigDecimal newBuyQuantity = buyOrder.getQuantity().subtract(executionQuantity);
        BigDecimal newSellQuantity = sellOrder.getQuantity().subtract(executionQuantity);
        
        updateOrder(buyOrder, newBuyQuantity, orderBook);
        updateOrder(sellOrder, newSellQuantity, orderBook);
    }
    
    private void updateOrder(Order order, BigDecimal newQuantity, IOrderBook orderBook) {
        order.updateQuantity(newQuantity);
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            orderBook.removeOrder(order);
        }
        orderRepository.updateOrder(order);
    }
    
    public void cancelOrder(String orderId) throws OrderException {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException("Order not found"));
        
        if (!order.isActive()) {
            throw new OrderException("Cannot cancel inactive order");
        }
        
        ReentrantLock lock = lockManager.acquireLock(orderId);
        try {
            // Recheck if order is still active after acquiring lock
            if (!order.isActive()) {
                throw new OrderException("Cannot cancel inactive order");
            }
            
            order.cancel();
            orderRepository.updateOrder(order);
            
            IOrderBook orderBook = orderBooks.get(order.getSymbol());
            if (orderBook != null) {
                orderBook.removeOrder(order);
            }
        } finally {
            lockManager.releaseLocks(lock);
        }
    }
    
    public IOrderBook getOrderBook(String symbol) {
        return orderBooks.get(symbol);
    }

    public void modifyOrder(String orderId, BigDecimal newPrice, BigDecimal newQuantity) throws OrderException {
        ValidationUtils.validateModifyOrderParameters(orderId, newPrice, newQuantity);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found"));

        if (!order.isActive()) {
            throw new OrderException("Cannot modify inactive order");
        }

        ReentrantLock lock = lockManager.acquireLock(orderId);
        try {
            // Recheck if order is still active after acquiring lock
            if (!order.isActive()) {
                throw new OrderException("Cannot modify inactive order");
            }

            IOrderBook orderBook = orderBooks.get(order.getSymbol());
            if (orderBook == null) {
                throw new OrderException("Order book not found for symbol: " + order.getSymbol());
            }

            orderBook.removeOrder(order);

            if (newPrice != null) {
                order.updatePrice(newPrice);
            }
            if (newQuantity != null) {
                order.updateQuantity(newQuantity);
            }

            orderRepository.updateOrder(order);
            orderBook.addOrder(order);

            matchOrders(orderBook);
        } finally {
            lockManager.releaseLocks(lock);
        }
    }
} 