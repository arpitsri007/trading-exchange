package com.phonepe.tradingexchange.common;

import java.time.Duration;

/**
 * Application-wide constants used across the trading platform.
 */
public final class Constants {
    // Prevent instantiation
    private Constants() {}
    
    // Timeouts and intervals
    public static final Duration ORDER_DEFAULT_EXPIRY = Duration.ofMinutes(5);
    public static final Duration ORDER_EXPIRY_CHECK_INTERVAL = Duration.ofSeconds(10);
    
    // Trading constants
    public static final int MAX_PRICE_PRECISION = 8;
    public static final int MAX_QUANTITY_PRECISION = 8;
    
    // Error messages
    public static final String ERROR_INVALID_SYMBOL = "Symbol cannot be null or empty";
    public static final String ERROR_INVALID_ORDER = "Order cannot be null";
    public static final String ERROR_SYMBOL_MISMATCH = "Order symbol must match orderbook symbol";
    public static final String ERROR_ORDER_NOT_FOUND = "Order not found";
    public static final String ERROR_INACTIVE_ORDER = "Cannot modify or cancel inactive order";
} 