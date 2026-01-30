package com.project.agent_brain_service.controller;

import com.project.agent_brain_service.MenuItem; // Import the new class
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/fake-swiggy")
public class FakeSwiggyController {

    // 1. A Smarter Database (Stores Price AND Cuisine)
    private static final Map<String, MenuItem> MENU = new HashMap<>();
    
    static {
        // Pizza is 600. If User Budget is 500, they CANNOT buy this without a coupon.
        MENU.put("pizza", new MenuItem(600.00, "Italian")); 
        MENU.put("burger", new MenuItem(150.50, "Fast Food"));
        MENU.put("sushi", new MenuItem(1200.00, "Japanese"));
        MENU.put("pasta", new MenuItem(450.00, "Italian"));
    }

    public static Set<String> getMenuItems() {
        return MENU.keySet();
    }
    
    // ✅ THIS IS THE METHOD YOU WERE MISSING
    public MenuItem getItemDetails(String itemName) {
        return MENU.get(itemName.toLowerCase());
    }

    @GetMapping("/menu")
    public Map<String, MenuItem> getMenu() {
        return MENU;
    }

    @PostMapping("/order")
    public String placeOrder(@RequestParam String item) {
        if (MENU.containsKey(item.toLowerCase())) {
            MenuItem details = MENU.get(item.toLowerCase());
            return "✅ ORDER CONFIRMED: " + item + " (" + details.cuisine + ") for $" + details.price + ". Driver is on the way!";
        } else {
            return "❌ OUT OF STOCK: We don't serve " + item + ".";
        }
    }
}