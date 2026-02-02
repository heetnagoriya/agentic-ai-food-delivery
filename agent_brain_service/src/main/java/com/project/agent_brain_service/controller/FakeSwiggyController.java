package com.project.agent_brain_service.controller;

import com.project.agent_brain_service.MenuItem;
import com.project.agent_brain_service.Order;
import com.project.agent_brain_service.Restaurant;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/fake-swiggy")
public class FakeSwiggyController {

    private static final List<Restaurant> RESTAURANTS = new ArrayList<>();
    private static final Map<String, Order> ACTIVE_ORDERS = new HashMap<>();

    static {
        // 🏪 1. Italian Bistro (Expensive, High Rated)
        Restaurant r1 = new Restaurant("res_1", "Luigi's Italian", true, 4.8);
        r1.addItem("Pizza", 600.0, "Italian", List.of("Cheesy", "Heavy"));
        r1.addItem("Pasta", 450.0, "Italian", List.of("Light", "Veg"));
        
        // 🏪 2. Burger King (Cheap, Fast Food)
        Restaurant r2 = new Restaurant("res_2", "Burger King", true, 4.2);
        r2.addItem("Burger", 150.0, "Fast Food", List.of("Spicy", "Non-Veg"));
        r2.addItem("Fries", 80.0, "Fast Food", List.of("Light", "Veg"));
        
        // 🏪 3. Healthy Hub (Closed right now!) - Tests "Restaurant Closed" logic
        Restaurant r3 = new Restaurant("res_3", "Healthy Hub", false, 4.9);
        r3.addItem("Khichdi", 200.0, "Healthy", List.of("Light", "Veg", "Indian"));
        r3.addItem("Salad", 250.0, "Healthy", List.of("Light", "Keto"));

        RESTAURANTS.add(r1);
        RESTAURANTS.add(r2);
        RESTAURANTS.add(r3);
    }

    // 🔍 1. SEARCH TOOL (Handles "Light food", "Pizza", "Khichdi")
    public List<Map<String, Object>> searchFood(String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        String q = query.toLowerCase();

        for (Restaurant r : RESTAURANTS) {
            for (MenuItem item : r.menu) {
                // Match Name OR Tags (This solves "Light food" query)
                boolean match = item.name.toLowerCase().contains(q) || 
                                item.tags.stream().anyMatch(t -> t.toLowerCase().contains(q));
                
                if (match) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("restaurant", r.name);
                    entry.put("restaurant_id", r.id);
                    entry.put("is_open", r.isOpen); // AI sees if it's closed
                    entry.put("item", item.name);
                    entry.put("price", item.price);
                    entry.put("rating", r.rating);
                    results.add(entry);
                }
            }
        }
        return results;
    }

    // 📦 2. PLACE ORDER (Requires Restaurant ID now)
    @PostMapping("/order")
    public String placeOrder(@RequestParam String restaurantId, @RequestParam String item) {
        // Find Restaurant
        Optional<Restaurant> resOpt = RESTAURANTS.stream()
                .filter(r -> r.id.equals(restaurantId)).findFirst();
        
        if (resOpt.isEmpty()) return "❌ Restaurant not found.";
        Restaurant res = resOpt.get();

        // Check Open/Close
        if (!res.isOpen) return "❌ FAILED: " + res.name + " is currently CLOSED.";

        // Find Item
        Optional<MenuItem> itemOpt = res.menu.stream()
                .filter(i -> i.name.equalsIgnoreCase(item)).findFirst();
        
        if (itemOpt.isPresent()) {
            Order newOrder = new Order(item + " from " + res.name); // 🆕 Includes Source
            ACTIVE_ORDERS.put(newOrder.orderId, newOrder);
            return "✅ ORDER CONFIRMED: " + item + " from " + res.name + ". ID: " + newOrder.orderId;
        }
        return "❌ Item not found in this restaurant.";
    }

    // ... (Keep getOrderStatus, cancelOrder, getLastOrderId same as before) ...
    public String getOrderStatus(String orderId) {
        if (ACTIVE_ORDERS.containsKey(orderId)) {
            Order order = ACTIVE_ORDERS.get(orderId);
            return "📦 STATUS: " + order.status + " | " + order.item + " | Est: " + order.deliveryTimeMins + "m";
        }
        return "❌ Order ID not found.";
    }

    public String cancelOrder(String orderId) {
        if (ACTIVE_ORDERS.containsKey(orderId)) {
            ACTIVE_ORDERS.remove(orderId);
            return "🚫 CANCELLED: " + orderId;
        }
        return "❌ Not found.";
    }
    
    public String getLastOrderId() {
        return ACTIVE_ORDERS.keySet().stream().reduce((f, s) -> s).orElse(null);
    }
    
    // Helper to get Item Details for Learning (Needed for AgentController)
    public MenuItem getItemDetails(String restaurantId, String itemName) {
         Optional<Restaurant> res = RESTAURANTS.stream().filter(r -> r.id.equals(restaurantId)).findFirst();
         if(res.isPresent()) {
             return res.get().menu.stream().filter(i -> i.name.equalsIgnoreCase(itemName)).findFirst().orElse(null);
         }
         return null;
    }
}