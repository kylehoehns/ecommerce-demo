package com.kylehoehns.ecommerce.order;

import com.kylehoehns.ecommerce.inventory.InventoryService;
import com.kylehoehns.ecommerce.notification.CustomerNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final InventoryService inventoryService;
    private final CustomerNotificationService notificationService;
    private final AtomicLong orderCounter = new AtomicLong(1000);

    public OrderService(InventoryService inventoryService, CustomerNotificationService notificationService) {
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;

        // seed orders with some fake data
        orders.put("ORD-123", new Order("ORD-123", "shirt-l", BigDecimal.ONE));
        orders.put("ORD-456", new Order("ORD-456", "shirt-m", BigDecimal.TEN));
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class InsufficientInventoryException extends RuntimeException {
        private final int available;
        
        public InsufficientInventoryException(String message, int available) {
            super(message);
            this.available = available;
        }
        
        public int getAvailable() {
            return available;
        }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }

    private String generateOrderId() {
        return "ORD-" + orderCounter.incrementAndGet();
    }

    private void validateOrderData(String sku, int quantity, BigDecimal price) {
        if (sku == null || sku.isBlank()) {
            throw new ValidationException("SKU is required");
        }
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be positive");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price must be positive");
        }
    }

    public Order createOrderWithValidation(String sku, int quantity, BigDecimal price) {
        validateOrderData(sku, quantity, price);

        var currentStock = inventoryService.getQuantity(sku);
        if (currentStock < quantity) {
            throw new InsufficientInventoryException("Insufficient inventory", currentStock);
        }

        inventoryService.remove(sku, quantity);
        var orderId = generateOrderId();
        var order = createOrder(orderId, sku, price);

        var message = "Order " + orderId + " created successfully for " + quantity + " units of " + sku + " at $" + price + " each";
        notificationService.informCustomer(message);

        return order;
    }

    public String processRefund(String orderId, String sku, BigDecimal price) {
        if (sku == null || sku.isBlank()) {
            throw new ValidationException("SKU is required");
        }

        inventoryService.add(sku, 1);
        var originalOrder = new Order(orderId, sku, price);
        issueRefund(originalOrder);

        var message = "Refund processed for order " + orderId + ". One " + sku + " returned to inventory";
        notificationService.informCustomer(message);

        return "Refund processed successfully";
    }

    public Order processReplacement(String orderId, String originalSku, String newSku, BigDecimal newPrice) {
        if (originalSku == null || originalSku.isBlank()) {
            throw new ValidationException("Original SKU is required");
        }
        if (newSku == null || newSku.isBlank()) {
            throw new ValidationException("New SKU is required");
        }

        var newStock = inventoryService.getQuantity(newSku);
        if (newStock <= 0) {
            throw new InsufficientInventoryException("Insufficient inventory for replacement", newStock);
        }

        inventoryService.add(originalSku, 1);
        inventoryService.remove(newSku, 1);

        var replacementOrder = new Order(generateOrderId(), newSku, newPrice);
        createReplacementOrder(replacementOrder);

        var message = "Replacement order created for " + orderId + ". Exchanging one " + originalSku + " for " + newSku;
        notificationService.informCustomer(message);

        return replacementOrder;
    }

    public Order createOrder(String orderId, String sku, BigDecimal price) {
        Order order = new Order(orderId, sku, price);
        orders.put(orderId, order);
        return order;
    }

    public Optional<Order> getOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Collection<Order> getAllOrders() {
        return orders.values();
    }

    public boolean cancelOrder(String orderId) {
        return orders.remove(orderId) != null;
    }

    public void createReplacementOrder(Order order) {
        orders.put(order.id(), order);
        logger.info("Creating replacement order for order {}", order.id());
    }

    public void issueRefund(Order order) {
        logger.info("Issuing refund for order {}", order.id());
        orders.remove(order.id());
    }
}
