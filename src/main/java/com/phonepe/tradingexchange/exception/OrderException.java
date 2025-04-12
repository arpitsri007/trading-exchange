package com.phonepe.tradingexchange.exception;

public class OrderException extends TradingException {
    public OrderException(String message) {
        super(message);
    }
    
    public OrderException(String message, Throwable cause) {
        super(message, cause);
    }
} 