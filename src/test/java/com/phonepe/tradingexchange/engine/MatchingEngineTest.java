package com.phonepe.tradingexchange.engine;

import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.model.OrderSide;
import com.phonepe.tradingexchange.model.Trade;
import com.phonepe.tradingexchange.repository.OrderRepository;
import com.phonepe.tradingexchange.repository.TradeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchingEngineTest {
    private MatchingEngine matchingEngine;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private TradeRepository tradeRepository;
    
    private static final String SYMBOL = "AAPL";
    private static final String USER_ID = "user1";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        MatchingEngine.reset();
        OrderRepository.reset();
        matchingEngine = MatchingEngine.getInstance();
        matchingEngine.setRepositories(orderRepository, tradeRepository);
    }

    @AfterEach
    void tearDown() {
        MatchingEngine.reset();
        OrderRepository.reset();
    }

    @Test
    void testPlaceOrderWithNullOrder() {
        assertThrows(OrderException.class, () -> matchingEngine.placeOrder(null));
    }

    @Test
    void testPlaceOrderWithInvalidPrice() {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(-100), BigDecimal.valueOf(10));
        assertThrows(OrderException.class, () -> matchingEngine.placeOrder(order));
    }

    @Test
    void testPlaceOrderWithInvalidQuantity() {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(-10));
        assertThrows(OrderException.class, () -> matchingEngine.placeOrder(order));
    }

    @Test
    void testPlaceOrderSuccess() throws OrderException {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        
        matchingEngine.placeOrder(order);
        
        verify(orderRepository).save(order);
        verify(orderRepository, never()).updateOrder(order);
    }

    @Test
    void testMatchOrders() throws OrderException {
        Order buyOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                         BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        Order sellOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                          BigDecimal.valueOf(90), BigDecimal.valueOf(10));
        
        when(orderRepository.findById(any())).thenReturn(java.util.Optional.of(buyOrder));
        
        matchingEngine.placeOrder(buyOrder);
        matchingEngine.placeOrder(sellOrder);
        
        verify(tradeRepository).addTrade(any(Trade.class));
        verify(orderRepository, times(2)).updateOrder(any(Order.class));
    }

    @Test
    void testPartialMatchOrders() throws OrderException {
        Order buyOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                         BigDecimal.valueOf(100), BigDecimal.valueOf(20));
        Order sellOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                          BigDecimal.valueOf(90), BigDecimal.valueOf(10));
        
        when(orderRepository.findById(any())).thenReturn(java.util.Optional.of(buyOrder));
        
        matchingEngine.placeOrder(buyOrder);
        matchingEngine.placeOrder(sellOrder);
        
        verify(tradeRepository).addTrade(any(Trade.class));
        verify(orderRepository, times(2)).updateOrder(any(Order.class));
    }

    @Test
    void testCancelOrder() throws OrderException {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        
        when(orderRepository.findById(order.getOrderId())).thenReturn(java.util.Optional.of(order));
        
        matchingEngine.cancelOrder(order.getOrderId());
        
        verify(orderRepository).updateOrder(order);
    }

    @Test
    void testCancelNonExistentOrder() {
        String orderId = "non-existent-order";
        when(orderRepository.findById(orderId)).thenReturn(java.util.Optional.empty());
        
        assertThrows(OrderException.class, () -> matchingEngine.cancelOrder(orderId));
    }

    @Test
    void testCancelInactiveOrder() throws OrderException {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        order.updateQuantity(BigDecimal.ZERO); // Mark as executed
        
        when(orderRepository.findById(order.getOrderId())).thenReturn(java.util.Optional.of(order));
        
        assertThrows(OrderException.class, () -> matchingEngine.cancelOrder(order.getOrderId()));
    }

    @Test
    void testGetOrderBook() {
        IOrderBook orderBook = matchingEngine.getOrderBook(SYMBOL);
        assertNull(orderBook);
        
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        try {
            matchingEngine.placeOrder(order);
        } catch (OrderException e) {
            fail("Should not throw exception");
        }
        
        orderBook = matchingEngine.getOrderBook(SYMBOL);
        assertNotNull(orderBook);
        assertEquals(SYMBOL, orderBook.getSymbol());
    }

    @Test
    void testConcurrentOrderMatching() throws OrderException {
        Order buyOrder1 = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                          BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        Order buyOrder2 = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                          BigDecimal.valueOf(95), BigDecimal.valueOf(5));
        Order sellOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                          BigDecimal.valueOf(90), BigDecimal.valueOf(15));
        
        when(orderRepository.findById(any())).thenReturn(java.util.Optional.of(buyOrder1));
        
        matchingEngine.placeOrder(buyOrder1);
        matchingEngine.placeOrder(buyOrder2);
        matchingEngine.placeOrder(sellOrder);
        
        verify(tradeRepository, times(2)).addTrade(any(Trade.class));
        verify(orderRepository, times(4)).updateOrder(any(Order.class));
    }

    @Test
    void testModifyOrderPrice() throws OrderException {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        
        when(orderRepository.findById(order.getOrderId())).thenReturn(java.util.Optional.of(order));
        
        matchingEngine.placeOrder(order);
        matchingEngine.modifyOrder(order.getOrderId(), BigDecimal.valueOf(110), null);
        
        verify(orderRepository, times(1)).updateOrder(order);
    }

    @Test
    void testModifyOrderQuantity() throws OrderException {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        
        when(orderRepository.findById(order.getOrderId())).thenReturn(java.util.Optional.of(order));
        
        matchingEngine.placeOrder(order);
        matchingEngine.modifyOrder(order.getOrderId(), null, BigDecimal.valueOf(20));
        
        verify(orderRepository, times(1)).updateOrder(order);
    }

    @Test
    void testModifyOrderPriceAndQuantity() throws OrderException {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        
        when(orderRepository.findById(order.getOrderId())).thenReturn(java.util.Optional.of(order));
        
        matchingEngine.placeOrder(order);
        matchingEngine.modifyOrder(order.getOrderId(), BigDecimal.valueOf(110), BigDecimal.valueOf(20));
        
        verify(orderRepository, times(1)).updateOrder(order);
    }

    @Test
    void testModifyNonExistentOrder() {
        String orderId = "non-existent-order";
        when(orderRepository.findById(orderId)).thenReturn(java.util.Optional.empty());
        
        assertThrows(OrderException.class, () -> 
            matchingEngine.modifyOrder(orderId, BigDecimal.valueOf(110), null));
    }

    @Test
    void testModifyInactiveOrder() throws OrderException {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        order.updateQuantity(BigDecimal.ZERO); // Mark as executed
        
        when(orderRepository.findById(order.getOrderId())).thenReturn(java.util.Optional.of(order));
        
        assertThrows(OrderException.class, () -> 
            matchingEngine.modifyOrder(order.getOrderId(), BigDecimal.valueOf(110), null));
    }

    @Test
    void testModifyOrderWithInvalidPrice() throws OrderException {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        
        when(orderRepository.findById(order.getOrderId())).thenReturn(java.util.Optional.of(order));
        
        assertThrows(OrderException.class, () -> 
            matchingEngine.modifyOrder(order.getOrderId(), BigDecimal.valueOf(-110), null));
    }

    @Test
    void testModifyOrderWithInvalidQuantity() throws OrderException {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        
        when(orderRepository.findById(order.getOrderId())).thenReturn(java.util.Optional.of(order));
        
        assertThrows(OrderException.class, () -> 
            matchingEngine.modifyOrder(order.getOrderId(), null, BigDecimal.valueOf(-20)));
    }

    @Test
    void testModifyOrderTriggersMatching() throws OrderException {
        Order buyOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                         BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        Order sellOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                          BigDecimal.valueOf(110), BigDecimal.valueOf(10));
        
        when(orderRepository.findById(any())).thenReturn(java.util.Optional.of(buyOrder));
        
        matchingEngine.placeOrder(buyOrder);
        matchingEngine.placeOrder(sellOrder);
        
        // Modify buy order price to match sell order
        matchingEngine.modifyOrder(buyOrder.getOrderId(), BigDecimal.valueOf(110), null);
        
        verify(tradeRepository).addTrade(any(Trade.class));
    }
} 