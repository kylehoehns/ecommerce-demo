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
        orders.put("ORD-123", new Order("ORD-123", "shirt-l"));
        orders.put("ORD-456", new Order("ORD-456", "shirt-m"));
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

    private void validateOrderData(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new ValidationException("SKU is required");
        }
    }

    public Order createOrderWithValidation(String sku) {
        validateOrderData(sku);

        var currentStock = inventoryService.getQuantity(sku);
        if (currentStock < 1) {
            throw new InsufficientInventoryException("Insufficient inventory", currentStock);
        }

        inventoryService.remove(sku);
        var orderId = generateOrderId();
        var order = createOrder(orderId, sku);

        var message = "Order " + orderId + " created successfully for 1 units of " + sku;
        notificationService.informCustomer(message);

        return order;
    }

    public String processRefund(String orderId) {
        var originalOrder = getOrder(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        inventoryService.add(originalOrder.sku());
        issueRefund(originalOrder);

        var message = "Refund processed for order " + orderId + ". One " + originalOrder.sku() + " returned to inventory";
        notificationService.informCustomer(message);

        return "Refund processed successfully";
    }

    public Order processReplacement(String orderId) {
        var order = getOrder(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        var newStock = inventoryService.getQuantity(order.sku());
        if (newStock <= 0) {
            throw new InsufficientInventoryException("Insufficient inventory for replacement", newStock);
        }

        inventoryService.remove(order.sku());

        var replacementOrder = new Order(generateOrderId(), order.sku());
        createReplacementOrder(replacementOrder);

        var message = "Replacement order created for " + orderId + ". Exchanging one " + order.sku();
        notificationService.informCustomer(message);

        return replacementOrder;
    }

    public Order createOrder(String orderId, String sku) {
        Order order = new Order(orderId, sku);
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

    private void createReplacementOrder(Order order) {
        orders.put(order.id(), order);
        logger.info("Creating replacement order for order {}", order.id());
    }

    private void issueRefund(Order order) {
        logger.info("Issuing refund for order {}", order.id());
        orders.remove(order.id());
    }
}
