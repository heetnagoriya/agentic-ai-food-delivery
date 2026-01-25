package com.project.agent_brain_service.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fake-swiggy")
public class FakeSwiggyController {

    // 1. A Hardcoded Menu (The "Database")
    private static final Map<String, Double> MENU = new HashMap<>();
    static {
        MENU.put("pizza", 12.99);
        MENU.put("burger", 8.50);
        MENU.put("sushi", 15.00);
        MENU.put("pasta", 10.00);
    }

    // Endpoint 1: Get the Menu
    // URL: http://localhost:8080/fake-swiggy/menu
    @GetMapping("/menu")
    public Map<String, Double> getMenu() {
        return MENU;
    }

    // Endpoint 2: Place an Order
    // URL: http://localhost:8080/fake-swiggy/order?item=pizza
    @PostMapping("/order")
    public String placeOrder(@RequestParam String item) {
        if (MENU.containsKey(item.toLowerCase())) {
            return "✅ ORDER CONFIRMED: " + item + " for $" + MENU.get(item.toLowerCase()) + ". Driver is on the way!";
        } else {
            return "❌ OUT OF STOCK: We don't serve " + item + ". Try pizza or burger.";
        }
    }
}