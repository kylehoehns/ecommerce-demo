package com.kylehoehns.ecommerce.inventory;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryService {
    private final Map<String, Integer> stock = new ConcurrentHashMap<>();

    public InventoryService() {
        // seed with sample data
        stock.put("shirt-s", 0);
        stock.put("shirt-m", 6);
        stock.put("shirt-l", 0);
        stock.put("shirt-xl", 2);
    }


    public Map<String, Integer> listAll() {
        return stock;
    }

    public Integer getQuantity(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    public int add(String sku) {
        if (sku == null || sku.isBlank())  {
            return stock.getOrDefault(sku, 0);
        }
        return stock.merge(sku, 1, Integer::sum);
    }


    public int remove(String sku) {
        if (sku == null || sku.isBlank()) {
            return stock.getOrDefault(sku, 0);
        }
        int current = stock.getOrDefault(sku, 0);
        int newQty = Math.max(0, current - 1);
        if (newQty == 0) {
            stock.remove(sku);
        } else {
            stock.put(sku, newQty);
        }
        return newQty;
    }

    public int set(String sku, int qty) {
        if (sku == null || sku.isBlank()) {
            return stock.getOrDefault(sku, 0);
        }
        if (qty <= 0) {
            stock.remove(sku);
            return 0;
        }
        stock.put(sku, qty);
        return qty;
    }

    public boolean delete(String sku) {
        return stock.remove(sku) != null;
    }
}
