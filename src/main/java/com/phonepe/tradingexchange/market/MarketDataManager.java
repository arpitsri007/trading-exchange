package com.phonepe.tradingexchange.market;

import com.phonepe.tradingexchange.engine.IOrderBook;
import com.phonepe.tradingexchange.engine.MatchingEngine;
import com.phonepe.tradingexchange.engine.OrderBook;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages market data including price updates and triggering stop-loss/take-profit orders.
 */
public class MarketDataManager {
    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isRunning = false;
    
    private static MarketDataManager INSTANCE;
    
    private MarketDataManager() {}
    
    public static MarketDataManager getInstance() {
        if (INSTANCE == null) {
            synchronized (MarketDataManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MarketDataManager();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Updates the last price for a symbol and triggers stop-loss/take-profit orders.
     * 
     * @param symbol The trading symbol (e.g., "BTCUSD")
     * @param price The new price
     */
    public void updatePrice(String symbol, BigDecimal price) {
        lastPrices.put(symbol, price);
        
        // Get the order book for this symbol and check stop-loss/take-profit orders
        MatchingEngine engine = MatchingEngine.getInstance();
        IOrderBook orderBook = engine.getOrderBook(symbol);
        
        if (orderBook instanceof OrderBook) {
            ((OrderBook) orderBook).checkStopLossAndTakeProfit(price);
        }
    }
    
    /**
     * Gets the last known price for a symbol.
     * 
     * @param symbol The trading symbol
     * @return The last price, or null if no price is available
     */
    public BigDecimal getLastPrice(String symbol) {
        return lastPrices.get(symbol);
    }
    
    /**
     * Starts periodic checking of stop-loss and take-profit orders.
     * 
     * @param intervalMs The interval in milliseconds between checks
     */
    public void startPeriodicChecks(long intervalMs) {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        scheduler.scheduleAtFixedRate(() -> {
            MatchingEngine engine = MatchingEngine.getInstance();
            
            // For each symbol with a known price, check stop-loss/take-profit orders
            lastPrices.forEach((symbol, price) -> {
                IOrderBook orderBook = engine.getOrderBook(symbol);
                if (orderBook instanceof OrderBook) {
                    ((OrderBook) orderBook).checkStopLossAndTakeProfit(price);
                }
            });
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Stops the periodic checking of stop-loss and take-profit orders.
     */
    public void stopPeriodicChecks() {
        if (!isRunning) {
            return;
        }
        
        scheduler.shutdown();
        isRunning = false;
    }
    
    /**
     * Resets the manager - primarily for testing.
     */
    public static void reset() {
        if (INSTANCE != null && INSTANCE.isRunning) {
            INSTANCE.stopPeriodicChecks();
        }
        INSTANCE = null;
    }
} 