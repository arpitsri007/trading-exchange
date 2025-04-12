package com.phonepe.tradingexchange.repository;

import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.model.OrderSide;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderRepository {
    private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
    private static final OrderRepository INSTANCE = new OrderRepository();
    
    private OrderRepository() {}
    
    public static OrderRepository getInstance() {
        return INSTANCE;
    }

    public static void reset() {
        INSTANCE.orders.clear();
    }

    public void save(Order order) {
        orders.put(order.getOrderId(), order);
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public List<Order> findByUserId(String userId) {
        return orders.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public void updateOrder(Order order) {
        if (orders.containsKey(order.getOrderId())) {
            orders.put(order.getOrderId(), order);
        }
    }

    public void cancelOrder(String orderId) {
        orders.computeIfPresent(orderId, (id, order) -> {
            order.cancel();
            return order;
        });
    }
}