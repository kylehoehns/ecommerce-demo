package com.kylehoehns.ecommerce.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/orders")
public class OrderRestController {

    private final OrderService orderService;

    public OrderRestController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        try {
            var order = orderService.createOrderWithValidation(request.sku(), request.quantity());
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (OrderService.ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (OrderService.InsufficientInventoryException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage() + ". Available: " + e.getAvailable());
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId) {
        return orderService.cancelOrder(orderId)
            ? ResponseEntity.ok("Order cancelled successfully")
            : ResponseEntity.notFound().build();
    }

    @PostMapping("/{orderId}/refund")
    public ResponseEntity<?> refundOrder(@PathVariable String orderId) {
        try {
            var result = orderService.processRefund(orderId);
            return ResponseEntity.ok(result);
        } catch (OrderService.OrderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (OrderService.ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{orderId}/replace")
    public ResponseEntity<?> replaceOrder(@PathVariable String orderId) {
        try {
            var order = orderService.processReplacement(orderId);
            return ResponseEntity.ok(order);
        } catch (OrderService.ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (OrderService.InsufficientInventoryException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage() + ". Available: " + e.getAvailable());
        }
    }

    public record OrderRequest(String sku, int quantity) {
    }

    public record ReplacementRequest(String originalSku) {
    }
}

