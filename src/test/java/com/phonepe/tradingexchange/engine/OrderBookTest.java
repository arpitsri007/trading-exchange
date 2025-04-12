package com.phonepe.tradingexchange.engine;

import com.phonepe.tradingexchange.exception.OrderException;
import com.phonepe.tradingexchange.model.Order;
import com.phonepe.tradingexchange.model.OrderSide;
import com.phonepe.tradingexchange.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {
    private OrderBook orderBook;
    private static final String SYMBOL = "AAPL";
    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        OrderRepository.reset();
        orderBook = new OrderBook(SYMBOL);
    }

    @AfterEach
    void tearDown() {
        OrderRepository.reset();
    }

    @Test
    void testConstructorWithNullSymbol() {
        assertThrows(OrderException.class, () -> new OrderBook(null));
    }

    @Test
    void testConstructorWithEmptySymbol() {
        assertThrows(OrderException.class, () -> new OrderBook(""));
    }

    @Test
    void testAddOrderWithNullOrder() {
        assertThrows(OrderException.class, () -> orderBook.addOrder(null));
    }

    @Test
    void testAddOrderWithWrongSymbol() {
        Order order = Order.createOrder(USER_ID, "GOOGL", OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        assertThrows(OrderException.class, () -> orderBook.addOrder(order));
    }

    @Test
    void testAddAndRemoveOrder() {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        
        orderBook.addOrder(order);
        assertEquals(1, orderBook.getTotalOrders());
        
        orderBook.removeOrder(order);
        assertEquals(0, orderBook.getTotalOrders());
    }

    @Test
    void testGetBestBid() {
        Order order1 = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                       BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        Order order2 = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                       BigDecimal.valueOf(110), BigDecimal.valueOf(5));
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        assertEquals(BigDecimal.valueOf(110), orderBook.getBestBid());
    }

    @Test
    void testGetBestBidWithNoOrders() {
        assertEquals(BigDecimal.ZERO, orderBook.getBestBid());
    }

    @Test
    void testGetBestAsk() {
        Order order1 = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                       BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        Order order2 = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                       BigDecimal.valueOf(90), BigDecimal.valueOf(5));
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        assertEquals(BigDecimal.valueOf(90), orderBook.getBestAsk());
    }

    @Test
    void testGetBestAskWithNoOrders() {
        assertEquals(BigDecimal.ZERO, orderBook.getBestAsk());
    }

    @Test
    void testHasMatchingOrders() {
        Order buyOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                         BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        Order sellOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                          BigDecimal.valueOf(90), BigDecimal.valueOf(5));
        
        orderBook.addOrder(buyOrder);
        orderBook.addOrder(sellOrder);
        
        assertTrue(orderBook.hasMatchingOrders());
    }

    @Test
    void testNoMatchingOrders() {
        Order buyOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                         BigDecimal.valueOf(90), BigDecimal.valueOf(10));
        Order sellOrder = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                          BigDecimal.valueOf(100), BigDecimal.valueOf(5));
        
        orderBook.addOrder(buyOrder);
        orderBook.addOrder(sellOrder);
        
        assertFalse(orderBook.hasMatchingOrders());
    }

    @Test
    void testHasMatchingOrdersWithNoOrders() {
        assertFalse(orderBook.hasMatchingOrders());
    }

    @Test
    void testGetNextBuyOrder() {
        Order order1 = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                       BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        Order order2 = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                       BigDecimal.valueOf(110), BigDecimal.valueOf(5));
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        assertEquals(order2, orderBook.getNextBuyOrder());
    }

    @Test
    void testGetNextBuyOrderWithNoOrders() {
        assertNull(orderBook.getNextBuyOrder());
    }

    @Test
    void testGetNextSellOrder() {
        Order order1 = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                       BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        Order order2 = Order.createOrder(USER_ID, SYMBOL, OrderSide.SELL, 
                                       BigDecimal.valueOf(90), BigDecimal.valueOf(5));
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        assertEquals(order2, orderBook.getNextSellOrder());
    }

    @Test
    void testGetNextSellOrderWithNoOrders() {
        assertNull(orderBook.getNextSellOrder());
    }

    @Test
    void testOrderPriority() {
        Order order1 = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                       BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        Order order2 = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                       BigDecimal.valueOf(100), BigDecimal.valueOf(5));
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        
        assertEquals(order1, orderBook.getNextBuyOrder());
    }

    @Test
    void testRemoveNonExistentOrder() {
        Order order = Order.createOrder(USER_ID, SYMBOL, OrderSide.BUY, 
                                      BigDecimal.valueOf(100), BigDecimal.valueOf(10));
        
        assertDoesNotThrow(() -> orderBook.removeOrder(order));
    }
} 