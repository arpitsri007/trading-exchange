# Trading Exchange

A high-performance, in-memory trading exchange system that supports order matching, modification, and cancellation.

## Architecture

The system follows a layered architecture with clear separation of concerns. For detailed architecture diagrams, please refer to:

- [Component Diagram](docs/component-diagram.puml) - Shows the high-level architecture and component relationships
- [Sequence Diagrams](docs/sequence-diagrams.puml) - Shows the flow of operations for:
  - Placing orders
  - Modifying orders
  - Cancelling orders

### Components

1. **Service Layer**
   - `TradingService`: Main service interface for all trading operations
   - Handles user registration, order placement, modification, and cancellation
   - Validates business rules and parameters

2. **Engine Layer**
   - `MatchingEngine`: Core engine for order matching and execution
   - `OrderBook`: Maintains buy and sell orders for each symbol
   - `IOrderBook`: Interface defining order book operations

3. **Repository Layer**
   - `OrderRepository`: Manages order storage and retrieval
   - `TradeRepository`: Manages trade storage and retrieval
   - `UserRepository`: Manages user storage and retrieval

4. **Model Layer**
   - `Order`: Represents a trading order
   - `Trade`: Represents an executed trade
   - `User`: Represents a system user
   - `OrderSide`: Enum for BUY/SELL sides
   - `OrderStatus`: Enum for order statuses (OPEN, EXECUTED, CANCELLED)

### Key Features

1. **Order Management**
   - Place new orders
   - Modify existing orders (price and/or quantity)
   - Cancel orders
   - View user orders

2. **Order Matching**
   - Price-time priority matching
   - Partial order execution
   - Concurrent order processing
   - Symbol-level locking for thread safety

3. **Market Data**
   - View best bid/ask prices
   - View trade history
   - View user trade history

## User Journey

### 1. User Registration
```java
User user = tradingService.registerUser("name", "email@example.com");
```

### 2. Placing Orders
```java
// Place a buy order
Order buyOrder = tradingService.placeOrder(
    userId,
    "AAPL",
    OrderSide.BUY,
    new BigDecimal("150.00"),
    new BigDecimal("100")
);

// Place a sell order
Order sellOrder = tradingService.placeOrder(
    userId,
    "AAPL",
    OrderSide.SELL,
    new BigDecimal("151.00"),
    new BigDecimal("50")
);
```

### 3. Modifying Orders
```java
// Modify order price
tradingService.modifyOrder(orderId, new BigDecimal("151.00"), null);

// Modify order quantity
tradingService.modifyOrder(orderId, null, new BigDecimal("75"));

// Modify both price and quantity
tradingService.modifyOrder(orderId, new BigDecimal("152.00"), new BigDecimal("75"));
```

### 4. Cancelling Orders
```java
tradingService.cancelOrder(orderId);
```

### 5. Viewing Market Data
```java
// Get best bid/ask prices
String marketData = tradingService.getMarketData("AAPL");

// Get trade history
List<Trade> trades = tradingService.getSymbolTrades("AAPL");

// Get user trades
List<Trade> userTrades = tradingService.getUserTrades(userId);

// Get user orders
List<Order> userOrders = tradingService.getUserOrders(userId);
```

## Design Patterns and Principles

1. **Singleton Pattern**
   - Used for service and repository instances
   - Ensures single instance across the application

2. **Factory Pattern**
   - Used for creating orders and trades
   - Encapsulates object creation logic

3. **Repository Pattern**
   - Abstracts data storage and retrieval
   - Provides clean interface for data access

4. **SOLID Principles**
   - Single Responsibility: Each class has one reason to change
   - Open/Closed: Components are open for extension
   - Liskov Substitution: Interfaces are properly implemented
   - Interface Segregation: Small, focused interfaces
   - Dependency Inversion: High-level modules depend on abstractions

5. **Concurrency**
   - Symbol-level locking for thread safety
   - ConcurrentHashMap for thread-safe storage
   - ReentrantReadWriteLock for order book operations

## Getting Started

1. Clone the repository
2. Build the project using Maven
3. Run the demo application
4. Explore the test cases for more examples

## Testing

The system includes comprehensive test coverage for:
- Order placement
- Order modification
- Order cancellation
- Order matching
- Concurrency scenarios
- Error handling
