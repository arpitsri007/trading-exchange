package com.phonepe.tradingexchange.engine;

import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.model.Trade;
import com.phonepe.tradingexchange.repository.OrderRepository;
import com.phonepe.tradingexchange.repository.TradeRepository;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class MatchingEngine {
    private final ConcurrentHashMap<String, IOrderBook> orderBooks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> symbolLocks = new ConcurrentHashMap<>();
    private OrderRepository orderRepository;
    private TradeRepository tradeRepository;
    
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
    
    public void placeOrder(Order order) throws OrderException {
        validateOrder(order);
        
        IOrderBook orderBook = orderBooks.computeIfAbsent(
            order.getSymbol(), 
            symbol -> new OrderBook(symbol)
        );
        
        orderRepository.save(order);
        orderBook.addOrder(order);
        matchOrders(orderBook);
    }
    
    private void validateOrder(Order order) throws OrderException {
        if (order == null) {
            throw new OrderException("Order cannot be null");
        }
        if (order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Price must be positive");
        }
        if (order.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Quantity must be positive");
        }
    }
    
    private void matchOrders(IOrderBook orderBook) {
        String symbol = orderBook.getSymbol();
        ReentrantLock symbolLock = symbolLocks.computeIfAbsent(symbol, s -> new ReentrantLock());
        
        symbolLock.lock();
        try {
            while (orderBook.hasMatchingOrders()) {
                Order buyOrder = orderBook.getNextBuyOrder();
                Order sellOrder = orderBook.getNextSellOrder();
                
                if (buyOrder == null || sellOrder == null) break;
                
                BigDecimal executionPrice = sellOrder.getPrice();
                BigDecimal executionQuantity = buyOrder.getQuantity().min(sellOrder.getQuantity());
                
                Trade trade = Trade.createTrade(buyOrder, sellOrder, executionPrice, executionQuantity);
                tradeRepository.addTrade(trade);
                
                processOrderExecution(buyOrder, sellOrder, executionQuantity, orderBook);
            }
        } finally {
            symbolLock.unlock();
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
        
        order.cancel();
        orderRepository.updateOrder(order);
        
        IOrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook != null) {
            orderBook.removeOrder(order);
        }
    }
    
    public IOrderBook getOrderBook(String symbol) {
        return orderBooks.get(symbol);
    }

    public void modifyOrder(String orderId, BigDecimal newPrice, BigDecimal newQuantity) throws OrderException {
        if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Price must be positive");
        }
        
        if (newQuantity != null && newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Quantity must be positive");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found"));

        if (!order.isActive()) {
            throw new OrderException("Cannot modify inactive order");
        }

        String symbol = order.getSymbol();
        ReentrantLock symbolLock = symbolLocks.computeIfAbsent(symbol, s -> new ReentrantLock());
        
        symbolLock.lock();
        try {
            IOrderBook orderBook = orderBooks.get(symbol);
            if (orderBook == null) {
                throw new OrderException("Order book not found for symbol: " + symbol);
            }

            // Remove order from order book
            orderBook.removeOrder(order);

            // Update order with new values
            if (newPrice != null) {
                order.updatePrice(newPrice);
            }
            if (newQuantity != null) {
                order.updateQuantity(newQuantity);
            }

            // Update repository and add back to order book
            orderRepository.updateOrder(order);
            orderBook.addOrder(order);

            // Try to match orders
            matchOrders(orderBook);
        } finally {
            symbolLock.unlock();
        }
    }
} 