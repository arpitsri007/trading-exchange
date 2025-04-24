package com.phonepe.tradingexchange.config;

import com.phonepe.tradingexchange.common.Constants;
import com.phonepe.tradingexchange.engine.MatchingEngine;
import com.phonepe.tradingexchange.market.MarketDataManager;
import com.phonepe.tradingexchange.repository.OrderRepository;
import com.phonepe.tradingexchange.repository.TradeRepository;

/**
 * Central configuration class for the trading exchange application.
 * Handles initialization and configuration of system components.
 */
public class AppConfig {
    private static AppConfig INSTANCE;
    
    private AppConfig() {}
    
    public static AppConfig getInstance() {
        if (INSTANCE == null) {
            synchronized (AppConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppConfig();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Initializes the application with default settings.
     */
    public void initialize() {
        // Get instances of all required components
        OrderRepository orderRepository = OrderRepository.getInstance();
        TradeRepository tradeRepository = TradeRepository.getInstance();
        MatchingEngine matchingEngine = MatchingEngine.getInstance();
        MarketDataManager marketDataManager = MarketDataManager.getInstance();
        
        // Set up the matching engine with repositories
        matchingEngine.setRepositories(orderRepository, tradeRepository);
        
        // Start periodic checks for stop-loss and take-profit orders
        long checkIntervalMs = Constants.ORDER_EXPIRY_CHECK_INTERVAL.toMillis();
        marketDataManager.startPeriodicChecks(checkIntervalMs);
    }
    
    /**
     * Resets all components - primarily for testing.
     */
    public static void reset() {
        INSTANCE = null;
        OrderRepository.reset();
        TradeRepository.reset();
        MatchingEngine.reset();
        MarketDataManager.reset();
    }
} 