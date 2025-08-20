## E‑commerce Demo

A Spring Boot application demonstrating e-commerce domain services with clean service layer architecture.

### Architecture

Built with **Java 21** and designed for maintainability:
- **Service layer** contains all business logic and validation
- **Exception-based error handling** for clear failure modes
- **Constructor injection** for dependency management
- **Clean separation** between REST controllers and business logic

### Services

- **Order Management** (`src/main/java/com/khoehns/ecommerce/order/`) – 
  Complete CRUD operations for orders with in-memory storage. Supports creating, retrieving, updating, and cancelling orders with automatic inventory management.

- **Inventory** (`src/main/java/com/khoehns/ecommerce/inventory/`) –
  Thread-safe in-memory inventory tracking with concurrent stock operations.

- **Classification** (`src/main/java/com/khoehns/ecommerce/classification/`) –
  Intent and sentiment analysis for customer requests.

- **Notifications** (`src/main/java/com/khoehns/ecommerce/notification/`) –
  Customer communication service for order updates.

### API Endpoints

#### Order Management
- `POST /orders` - Create new order
- `GET /orders` - List all orders  
- `GET /orders/{id}` - Get specific order
- `PUT /orders/{id}` - Update order
- `DELETE /orders/{id}` - Cancel order
- `POST /orders/{id}/refund` - Process refund
- `POST /orders/{id}/replace` - Create replacement order

#### Inventory
- `GET /inventory` - List all inventory
- `GET /inventory/{sku}` - Get stock for SKU
- `POST /inventory/{sku}` - Add stock
- `DELETE /inventory/{sku}` - Remove stock

### Running the Application

Requires **Java 21** and Maven:

```sh
mvn spring-boot:run
```

Application starts on port 8080. Example usage:

```sh
# Create an order
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"sku": "shirt-m", "quantity": 2, "price": 25.99}'

# Check inventory
curl http://localhost:8080/inventory

# Get order details
curl http://localhost:8080/orders/{order-id}
```

### Features

- **In-memory persistence** - Orders and inventory persist during application runtime
- **Automatic inventory management** - Stock levels update with order operations
- **Thread-safe operations** - Uses `ConcurrentHashMap` for concurrent access
- **Service-layer validation** - Business logic and validation centralized in services
- **RESTful design** - Standard HTTP methods and status codes

### Setting up MCP Server in Claude Desktop

Add this to your claude_desktop_config.json file

```json
{
  "mcpServers": {
    "customer-support": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "http://localhost:8080/sse"
      ]
    }
  }
}
```