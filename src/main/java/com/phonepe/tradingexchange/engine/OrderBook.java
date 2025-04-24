package com.phonepe.tradingexchange.engine;

import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.model.OrderSide;
import com.phonepe.tradingexchange.model.OrderType;
import com.phonepe.tradingexchange.util.ValidationUtils;

import java.math.BigDecimal;
import java.util.PriorityQueue;

public class OrderBook implements IOrderBook {
    private final String symbol;
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;
    private final PriorityQueue<Order> stopLossOrders;
    private final PriorityQueue<Order> takeProfitOrders;
    
    public OrderBook(String symbol) {
        ValidationUtils.validateSymbol(symbol);
        
        this.symbol = symbol;

        this.buyOrders = new PriorityQueue<>((order1, order2) -> {
            int priceComparison = order2.getPrice().compareTo(order1.getPrice());
            if (priceComparison == 0) {
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return priceComparison;
        });
        
        this.sellOrders = new PriorityQueue<>((order1, order2) -> {
            int priceComparison = order1.getPrice().compareTo(order2.getPrice());
            if (priceComparison == 0) {
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return priceComparison;
        });

        this.stopLossOrders = new PriorityQueue<>((order1, order2) -> {
            int priceComparison = order1.getStopLossPrice().compareTo(order2.getStopLossPrice());
            if (priceComparison == 0) {
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return priceComparison;
        });

        this.takeProfitOrders = new PriorityQueue<>((order1, order2) -> {
            int priceComparison = order1.getTakeProfitPrice().compareTo(order2.getTakeProfitPrice());
            if (priceComparison == 0) {
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return priceComparison;
        });
    }
    
    @Override
    public void addOrder(Order order) {
        ValidationUtils.validateOrderSymbol(order, symbol);
        
        switch (order.getOrderType()) {
            case MARKET:
                if (order.getSide() == OrderSide.BUY) {
                    buyOrders.add(order);
                } else {
                    sellOrders.add(order);
                }
                break;
            case STOP_LOSS:
                stopLossOrders.add(order);
                break;
            case TAKE_PROFIT:
                takeProfitOrders.add(order);
                break;
        }
    }
    
    @Override
    public void removeOrder(Order order) {
        if (order == null) {
            throw new OrderException("Order cannot be null");
        }
        
        switch (order.getOrderType()) {
            case MARKET:
                if (order.getSide() == OrderSide.BUY) {
                    buyOrders.remove(order);
                } else {
                    sellOrders.remove(order);
                }
                break;
            case STOP_LOSS:
                stopLossOrders.remove(order);
                break;
            case TAKE_PROFIT:
                takeProfitOrders.remove(order);
                break;
        }
    }
    
    @Override
    public BigDecimal getBestBid() {
        Order bestBuy = buyOrders.peek();
        return bestBuy != null ? bestBuy.getPrice() : BigDecimal.ZERO;
    }
    
    @Override
    public BigDecimal getBestAsk() {
        Order bestSell = sellOrders.peek();
        return bestSell != null ? bestSell.getPrice() : BigDecimal.ZERO;
    }
    
    @Override
    public Order getNextBuyOrder() {
        return buyOrders.peek();
    }
    
    @Override
    public Order getNextSellOrder() {
        return sellOrders.peek();
    }
    
    @Override
    public boolean hasMatchingOrders() {
        Order bestBuy = buyOrders.peek();
        Order bestSell = sellOrders.peek();
        
        return bestBuy != null && bestSell != null && 
                bestBuy.getPrice().compareTo(bestSell.getPrice()) >= 0;
    }
    
    @Override
    public int getTotalOrders() {
        return buyOrders.size() + sellOrders.size() + 
               stopLossOrders.size() + takeProfitOrders.size();
    }
    
    @Override
    public String getSymbol() {
        return symbol;
    }

    public void checkStopLossAndTakeProfit(BigDecimal currentPrice) {
        // Check stop-loss orders
        while (!stopLossOrders.isEmpty()) {
            Order order = stopLossOrders.peek();
            if (order.isStopLossTriggered(currentPrice)) {
                stopLossOrders.poll();
                if (order.getSide() == OrderSide.BUY) {
                    buyOrders.add(order);
                } else {
                    sellOrders.add(order);
                }
            } else {
                break;
            }
        }

        // Check take-profit orders
        while (!takeProfitOrders.isEmpty()) {
            Order order = takeProfitOrders.peek();
            if (order.isTakeProfitTriggered(currentPrice)) {
                takeProfitOrders.poll();
                if (order.getSide() == OrderSide.BUY) {
                    buyOrders.add(order);
                } else {
                    sellOrders.add(order);
                }
            } else {
                break;
            }
        }
    }
} 