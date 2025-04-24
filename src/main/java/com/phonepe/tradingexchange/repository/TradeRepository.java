package com.phonepe.tradingexchange.repository;

import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.model.Trade;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TradeRepository {
    private final ConcurrentHashMap<String, Trade> trades = new ConcurrentHashMap<>();
    
    
    private static TradeRepository INSTANCE;
    
    private TradeRepository() {
       
    }
    
    public static TradeRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (TradeRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TradeRepository();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Resets the repository - primarily for testing.
     */
    public static void reset() {
        INSTANCE = null;
    }
    
    public void addTrade(Trade trade) {
        trades.put(trade.getTradeId(), trade);
    }
    
    public List<Trade> findBySymbol(String symbol) {
        return trades.values().stream()
                .filter(trade -> trade.getSymbol().equals(symbol))
                .collect(Collectors.toList());
    }
    
    public List<Trade> findByUserId(String userId) {
        OrderRepository orderRepo = OrderRepository.getInstance();
        return trades.values().stream()
                .filter(trade -> {
                    Optional<Order> buyOrder = orderRepo.findById(trade.getBuyOrderId());
                    Optional<Order> sellOrder = orderRepo.findById(trade.getSellOrderId());
                    return (buyOrder.isPresent() && buyOrder.get().getUserId().equals(userId)) ||
                            (sellOrder.isPresent() && sellOrder.get().getUserId().equals(userId));
                })
                .collect(Collectors.toList());
    }
    
    public int count() {
        return trades.size();
    }
} 