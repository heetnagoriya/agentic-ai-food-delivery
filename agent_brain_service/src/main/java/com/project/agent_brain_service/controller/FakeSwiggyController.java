package com.project.agent_brain_service.controller;

import com.project.agent_brain_service.MenuItem;
import com.project.agent_brain_service.Order;
import com.project.agent_brain_service.Restaurant;
import com.project.agent_brain_service.UserWallet;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/fake-swiggy")
public class FakeSwiggyController {

    private static final List<Restaurant> RESTAURANTS = new ArrayList<>();
    private static final Map<String, Order> ACTIVE_ORDERS = new HashMap<>();
    private static final Map<String, UserWallet> WALLETS = new HashMap<>();

    static {
        // --- 💰 WALLETS ---
        WALLETS.put("user_123", new UserWallet("user_123", 2500.0));
        WALLETS.put("user_456", new UserWallet("user_456", 100.0));

        // --- 🏪 RESTAURANTS & MENU EXPANSION ---

        // 1. Luigi's Italian (High End)
        Restaurant r1 = new Restaurant("res_1", "Luigi's Italian", true, 4.8, "Downtown");
        r1.addItem("Pizza", 600.0, "Italian", List.of("Cheesy", "Heavy", "Dinner"), 50);
        r1.addItem("Pasta Alfredo", 450.0, "Italian", List.of("Creamy", "Veg", "Dinner"), 5);
        r1.addItem("Tiramisu", 300.0, "Dessert", List.of("Sweet", "Coffee"), 20);

        // 2. Burger King (Fast Food)
        Restaurant r2 = new Restaurant("res_2", "Burger King", true, 4.2, "Uptown");
        r2.addItem("Whopper Burger", 150.0, "Fast Food", List.of("Spicy", "Non-Veg", "Lunch"), 100);
        r2.addItem("Fries", 80.0, "Fast Food", List.of("Light", "Veg", "Snack"), 200);
        r2.addItem("Coke", 60.0, "Beverage", List.of("Drink", "Cold"), 500);

        // 3. Spice Garden (South Indian - Dosa/Idli!)
        Restaurant r3 = new Restaurant("res_3", "Spice Garden", true, 4.6, "Midtown");
        r3.addItem("Masala Dosa", 120.0, "South Indian", List.of("Veg", "Crispy", "Breakfast"), 30);
        r3.addItem("Idli Sambhar", 90.0, "South Indian", List.of("Veg", "Healthy", "Breakfast"), 40);
        r3.addItem("Uttapam", 110.0, "South Indian", List.of("Veg", "Soft"), 25);

        // 4. Wok & Roll (Chinese)
        Restaurant r4 = new Restaurant("res_4", "Wok & Roll", true, 4.4, "Chinatown");
        r4.addItem("Hakka Noodles", 200.0, "Chinese", List.of("Veg", "Spicy", "Dinner"), 60);
        r4.addItem("Manchurian", 180.0, "Chinese", List.of("Veg", "Gravy"), 50);
        r4.addItem("Spring Rolls", 150.0, "Chinese", List.of("Starter", "Crispy"), 40);

        // 5. The Sweet Spot (Desserts - CLOSED right now to test logic)
        Restaurant r5 = new Restaurant("res_5", "The Sweet Spot", false, 4.9, "Mall");
        r5.addItem("Chocolate Cake", 500.0, "Dessert", List.of("Sweet", "Party"), 10);
        r5.addItem("Ice Cream", 100.0, "Dessert", List.of("Cold", "Summer"), 50);

        // 6. Healthy Hub (Diet Food)
        Restaurant r6 = new Restaurant("res_6", "Healthy Hub", true, 4.7, "Green Zone");
        r6.addItem("Khichdi", 200.0, "Healthy", List.of("Light", "Veg", "Indian", "Sick Food"), 10);
        r6.addItem("Greek Salad", 250.0, "Healthy", List.of("Light", "Keto", "Raw"), 20);
        r6.addItem("Fruit Bowl", 180.0, "Healthy", List.of("Sweet", "Natural"), 15);

        RESTAURANTS.add(r1);
        RESTAURANTS.add(r2);
        RESTAURANTS.add(r3);
        RESTAURANTS.add(r4);
        RESTAURANTS.add(r5);
        RESTAURANTS.add(r6);
    }

    // ... (Keep existing methods: searchFood, placeOrder, getWalletBalance, etc. unchanged) ...
    // Note: Ensure searchFood limits results to Top 5 to prevent Token Overflow!

    public List<Map<String, Object>> searchFood(@RequestParam(defaultValue = "") String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        String q = query.toLowerCase();

        for (Restaurant r : RESTAURANTS) {
            for (MenuItem item : r.menu) {
                boolean match = q.isEmpty() || item.name.toLowerCase().contains(q) || 
                                item.tags.stream().anyMatch(t -> t.toLowerCase().contains(q));
                if (match) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("restaurant", r.name);
                    entry.put("restaurant_id", r.id);
                    entry.put("is_open", r.isOpen);
                    entry.put("item", item.name);
                    entry.put("price", item.price);
                    entry.put("stock", item.stockCount);
                    entry.put("tags", item.tags);
                    results.add(entry);
                }
            }
        }
        return results.size() > 5 ? results.subList(0, 5) : results;
    }
    
    // ... (Paste the rest of the file from the previous step) ...
    
    // Helper to Get Wallet
    public double getWalletBalance(String userId) {
        return WALLETS.containsKey(userId) ? WALLETS.get(userId).balance : 0.0;
    }
    
   // 📦 UPDATED ORDER PROCESSOR (Accepts Discounted Price)
    @PostMapping("/order")
    public String placeOrder(@RequestParam String restaurantId, 
                             @RequestParam String item,
                             @RequestParam(defaultValue = "user_123") String userId,
                             @RequestParam(required = false) Double finalPrice) { // 🆕 Optional Price
        
        UserWallet wallet = WALLETS.get(userId);
        if (wallet == null) return "❌ DATA ERROR: User Wallet not found.";

        Optional<Restaurant> resOpt = RESTAURANTS.stream().filter(r -> r.id.equals(restaurantId)).findFirst();
        if (resOpt.isEmpty()) return "❌ ERROR: Restaurant ID invalid.";
        Restaurant res = resOpt.get();

        if (!res.isOpen) return "❌ STORE CLOSED: " + res.name + " is currently closed.";

        Optional<MenuItem> itemOpt = res.menu.stream().filter(i -> i.name.equalsIgnoreCase(item)).findFirst();
        if (itemOpt.isEmpty()) return "❌ ITEM NOT FOUND.";
        MenuItem food = itemOpt.get();

        if (food.stockCount <= 0) return "❌ OUT OF STOCK: " + food.name;

        // 🆕 PRICE LOGIC: Use provided price OR default menu price
        double amountToCharge = (finalPrice != null) ? finalPrice : food.price;

        if (!wallet.deductAmount(amountToCharge, item + " @ " + res.name)) {
            return "❌ PAYMENT DECLINED: Insufficient Funds. Bal: ₹" + wallet.balance;
        }

        food.stockCount--; 
        Order newOrder = new Order(item + " from " + res.name);
        ACTIVE_ORDERS.put(newOrder.orderId, newOrder);

        return "✅ ORDER SUCCESS: " + item + ". ID: " + newOrder.orderId + ". New Bal: ₹" + wallet.balance;
    }
    
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

    public MenuItem getItemDetails(String restaurantId, String itemName) {
         Optional<Restaurant> res = RESTAURANTS.stream().filter(r -> r.id.equals(restaurantId)).findFirst();
         if(res.isPresent()) {
             return res.get().menu.stream().filter(i -> i.name.equalsIgnoreCase(itemName)).findFirst().orElse(null);
         }
         return null;
    }
    // 👁️ OBSERVABILITY ENDPOINT (For the Dashboard)
    @GetMapping("/world-state")
    public Map<String, Object> getWorldState() {
        Map<String, Object> state = new HashMap<>();
        state.put("wallets", WALLETS);
        state.put("restaurants", RESTAURANTS);
        state.put("active_orders", ACTIVE_ORDERS);
        return state;
    }
}