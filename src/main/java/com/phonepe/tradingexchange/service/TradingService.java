package com.phonepe.tradingexchange.service;

import com.phonepe.tradingexchange.engine.MatchingEngine;
import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.exception.TradingException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.model.OrderSide;
import com.phonepe.tradingexchange.model.Trade;
import com.phonepe.tradingexchange.model.User;
import com.phonepe.tradingexchange.repository.OrderRepository;
import com.phonepe.tradingexchange.repository.TradeRepository;
import com.phonepe.tradingexchange.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

public class TradingService {
    private final MatchingEngine matchingEngine;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    
    private static TradingService INSTANCE;
    
    private TradingService() {
        this.matchingEngine = MatchingEngine.getInstance();
        this.userRepository = UserRepository.getInstance();
        this.orderRepository = OrderRepository.getInstance();
        this.tradeRepository = TradeRepository.getInstance();       
        this.matchingEngine.setRepositories(orderRepository, tradeRepository);
    }
    
    public static TradingService getInstance() {
        if (INSTANCE == null) {
            synchronized (TradingService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TradingService();
                }
            }
        }
        return INSTANCE;
    }

    public User registerUser(String name, String email) throws TradingException {
        try {
            validateUserDetails(name, email);
            User user = User.createUser(name, email);
            userRepository.addUser(user);
            return user;
        } catch (Exception e) {
            throw new TradingException("Failed to register user: " + e.getMessage(), e);
        }
    }
    
    private void validateUserDetails(String name, String email) throws TradingException {
        if (name == null || name.trim().isEmpty()) {
            throw new TradingException("Name cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new TradingException("Email cannot be null or empty");
        }
    }
    public Order placeOrder(String userId, String symbol, OrderSide side, 
                          BigDecimal price, BigDecimal quantity) throws OrderException {
        try {
            validateOrderParameters(userId, symbol, price, quantity);
            Order order = Order.createOrder(userId, symbol, side, price, quantity);
            matchingEngine.placeOrder(order);
            return order;
        } catch (OrderException e) {
            throw e;
        } catch (Exception e) {
            throw new OrderException("Failed to place order: " + e.getMessage(), e);
        }
    }
    
    private void validateOrderParameters(String userId, String symbol, 
                                       BigDecimal price, BigDecimal quantity) throws OrderException {
        if (!userRepository.existsById(userId)) {
            throw new OrderException("User not found");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new OrderException("Symbol cannot be null or empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Price must be positive");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Quantity must be positive");
        }
    }
    
    public void cancelOrder(String orderId) throws OrderException {
        try {
            matchingEngine.cancelOrder(orderId);
        } catch (OrderException e) {
            throw e;
        } catch (Exception e) {
            throw new OrderException("Failed to cancel order: " + e.getMessage(), e);
        }
    }
    
    public List<Order> getUserOrders(String userId) throws TradingException {
        try {
            return orderRepository.findByUserId(userId);
        } catch (Exception e) {
            throw new TradingException("Failed to get user orders: " + e.getMessage(), e);
        }
    }
    
    public List<Trade> getUserTrades(String userId) throws TradingException {
        try {
            return tradeRepository.findByUserId(userId);
        } catch (Exception e) {
            throw new TradingException("Failed to get user trades: " + e.getMessage(), e);
        }
    }
    public List<Trade> getSymbolTrades(String symbol) throws TradingException {
        try {
            return tradeRepository.findBySymbol(symbol);
        } catch (Exception e) {
            throw new TradingException("Failed to get symbol trades: " + e.getMessage(), e);
        }
    }
    public String getMarketData(String symbol) throws TradingException {
        try {
            var orderBook = matchingEngine.getOrderBook(symbol);
            if (orderBook == null) {
                return "No orders for symbol: " + symbol;
            }
            
            return String.format("Symbol: %s, Best Bid: %s, Best Ask: %s",
                    symbol,
                    orderBook.getBestBid(),
                    orderBook.getBestAsk());
        } catch (Exception e) {
            throw new TradingException("Failed to get market data: " + e.getMessage(), e);
        }
    }

    public void modifyOrder(String orderId, BigDecimal newPrice, BigDecimal newQuantity) throws OrderException {
        try {
            validateModifyOrderParameters(orderId, newPrice, newQuantity);
            matchingEngine.modifyOrder(orderId, newPrice, newQuantity);
        } catch (OrderException e) {
            throw e;
        } catch (Exception e) {
            throw new OrderException("Failed to modify order: " + e.getMessage(), e);
        }
    }

    private void validateModifyOrderParameters(String orderId, BigDecimal newPrice, BigDecimal newQuantity) throws OrderException {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new OrderException("Order ID cannot be null or empty");
        }
        if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Price must be positive");
        }
        if (newQuantity != null && newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Quantity must be positive");
        }
        if (newPrice == null && newQuantity == null) {
            throw new OrderException("At least one of price or quantity must be provided");
        }
    }
} 