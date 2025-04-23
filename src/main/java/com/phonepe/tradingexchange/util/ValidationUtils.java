package com.phonepe.tradingexchange.util;

import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.exception.TradingException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.repository.UserRepository;

import java.math.BigDecimal;

public class ValidationUtils {
    
    public static void validateUserDetails(String name, String email) throws TradingException {
        if (name == null || name.trim().isEmpty()) {
            throw new TradingException("Name cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new TradingException("Email cannot be null or empty");
        }
    }
    
    public static void validateOrderParameters(String userId, String symbol, 
                                             BigDecimal price, BigDecimal quantity) throws OrderException {
        if (!UserRepository.getInstance().existsById(userId)) {
            throw new OrderException("User not found");
        }
        validateSymbol(symbol);
        validatePrice(price);
        validateQuantity(quantity);
    }
    
    public static void validateModifyOrderParameters(String orderId, BigDecimal newPrice, 
                                                   BigDecimal newQuantity) throws OrderException {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new OrderException("Order ID cannot be null or empty");
        }
        if (newPrice != null) {
            validatePrice(newPrice);
        }
        if (newQuantity != null) {
            validateQuantity(newQuantity);
        }
        if (newPrice == null && newQuantity == null) {
            throw new OrderException("At least one of price or quantity must be provided");
        }
    }
    
    public static void validateSymbol(String symbol) throws OrderException {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new OrderException("Symbol cannot be null or empty");
        }
    }
    
    public static void validatePrice(BigDecimal price) throws OrderException {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Price must be positive");
        }
    }
    
    public static void validateQuantity(BigDecimal quantity) throws OrderException {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Quantity must be positive");
        }
    }

    public static void validateOrderSymbol(Order order, String expectedSymbol) throws OrderException {
        if (order == null) {
            throw new OrderException("Order cannot be null");
        }
        if (!order.getSymbol().equals(expectedSymbol)) {
            throw new OrderException("Order symbol does not match expected symbol");
        }
    }
} 