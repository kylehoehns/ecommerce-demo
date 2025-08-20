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
            var order = orderService.createOrderWithValidation(request.sku(), request.quantity(), request.price());
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

    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateOrder(@PathVariable String orderId, @RequestBody OrderRequest request) {
        try {
            var order = orderService.updateOrderWithValidation(orderId, request.sku(), request.quantity(), request.price());
            return ResponseEntity.ok(order);
        } catch (OrderService.ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (OrderService.OrderNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderId) {
        return orderService.cancelOrder(orderId)
            ? ResponseEntity.ok("Order cancelled successfully")
            : ResponseEntity.notFound().build();
    }

    @PostMapping("/{orderId}/refund")
    public ResponseEntity<?> refundOrder(@PathVariable String orderId, @RequestBody RefundRequest request) {
        try {
            var result = orderService.processRefund(orderId, request.sku(), request.quantity(), request.price());
            return ResponseEntity.ok(result);
        } catch (OrderService.ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{orderId}/replace")
    public ResponseEntity<?> replaceOrder(@PathVariable String orderId, @RequestBody ReplacementRequest request) {
        try {
            var order = orderService.processReplacement(orderId, request.originalSku(), request.newSku(),
                request.quantity(), request.newPrice());
            return ResponseEntity.ok(order);
        } catch (OrderService.ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (OrderService.InsufficientInventoryException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage() + ". Available: " + e.getAvailable());
        }
    }

    public record OrderRequest(String sku, int quantity, BigDecimal price) {
    }

    public record RefundRequest(String sku, int quantity, BigDecimal price) {
    }

    public record ReplacementRequest(String originalSku, String newSku, int quantity,
                                     BigDecimal newPrice) {
    }
}

