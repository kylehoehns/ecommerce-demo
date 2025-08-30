package com.kylehoehns.ecommerce.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryRestController {

    private final InventoryService inventoryService;

    public InventoryRestController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public record InventoryItem(String sku, int qty) {
    }

    public record InventoryMutation(String sku) {
    }

    @GetMapping
    public Map<String, Integer> listAll() {
        return inventoryService.listAll();
    }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryItem> getOne(@PathVariable String sku) {
        var qty = inventoryService.getQuantity(sku);
        if (qty <= 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new InventoryItem(sku, qty));
    }

    @PostMapping("/add")
    public ResponseEntity<InventoryItem> add(@RequestBody InventoryMutation req) {
        if (req.sku() == null || req.sku().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        int newQty = inventoryService.add(req.sku());
        return ResponseEntity.ok(new InventoryItem(req.sku(), newQty));
    }

    @PostMapping("/remove")
    public ResponseEntity<InventoryItem> remove(@RequestBody InventoryMutation req) {
        if (req.sku() == null || req.sku().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        int newQty = inventoryService.remove(req.sku());
        return ResponseEntity.ok(new InventoryItem(req.sku(), newQty));
    }

    @PutMapping("/{sku}")
    public ResponseEntity<InventoryItem> set(@PathVariable String sku, @RequestParam int qty) {
        if (sku == null || sku.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        int newQty = inventoryService.set(sku, qty);
        return ResponseEntity.ok(new InventoryItem(sku, Math.max(0, newQty)));
    }

    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> delete(@PathVariable String sku) {
        boolean removed = inventoryService.delete(sku);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
