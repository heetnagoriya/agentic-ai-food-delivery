package com.project.agent_brain_service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Service
public class UserProfileService {

    // Simulating a Database
    private static final Map<String, UserProfile> MOCK_DB = new HashMap<>();

    static {
        // ══════════════════════════════════════════════
        //   USER 123 — Rich, experienced, knows what they like
        // ══════════════════════════════════════════════
        UserProfile u1 = new UserProfile("user_123");

        // Preferences
        u1.preferences.cuisines.addAll(List.of("Italian", "Fast Food"));
        u1.preferences.likes.add("Spicy Pizza");
        u1.preferences.allergies.add("Peanuts");

        // 🆕 Dislikes & Blacklist
        u1.preferences.dislikes.addAll(List.of("Soggy crust", "Stale bun"));
        u1.preferences.blacklistedRestaurants.add("res_2"); // Blacklisted Burger King

        // 🆕 Cuisine Confidence (high for known cuisines, absent for unknown)
        u1.preferences.cuisineConfidence.put("Italian", 0.9);
        u1.preferences.cuisineConfidence.put("Fast Food", 0.8);
        // Note: "South Indian", "Indian" etc. are NOT present → confidence = 0.0 → agent should ask

        // Budget
        u1.budget.rangeMin = 400;
        u1.budget.rangeMax = 800;
        u1.budget.confidenceLevel = "high";

        // Ordering Behavior
        u1.orderingBehavior.avgOrderValue = 650;
        u1.orderingBehavior.totalOrders = 10;

        // 🆕 Autonomy Level
        u1.autonomyLevel = "BALANCED";

        // Restaurant Preferences (past positive experiences)
        u1.restaurantPreferences.put("res_1", 0.95);  // Loves Luigi's Italian (premium pizza)
        u1.restaurantPreferences.put("res_3", 0.7);   // Has ordered from Spice Garden before
        u1.restaurantPreferences.put("res_4", 0.6);   // Tried Pizza Hut, it's okay
        // Note: res_5 (McDonald's), res_6 (Dosa Plaza) — never ordered → default 0.5

        MOCK_DB.put("user_123", u1);

        // ══════════════════════════════════════════════
        //   USER 456 — Budget user, new, little history
        // ══════════════════════════════════════════════
        UserProfile u2 = new UserProfile("user_456");
        u2.budget.rangeMin = 50;
        u2.budget.rangeMax = 150;
        u2.budget.confidenceLevel = "medium";
        u2.orderingBehavior.avgOrderValue = 100;
        u2.orderingBehavior.totalOrders = 2;
        u2.autonomyLevel = "CONSERVATIVE"; // New user → always confirm

        // Budget user prefers affordable restaurants
        u2.restaurantPreferences.put("res_4", 0.8);   // Loves Pizza Hut (budget-friendly)
        u2.restaurantPreferences.put("res_5", 0.7);   // Likes McDonald's (cheap burgers)
        u2.restaurantPreferences.put("res_6", 0.75);  // Likes Dosa Plaza (cheapest dosa)

        MOCK_DB.put("user_456", u2);
    }

    // 1. READ
    public UserProfile getUserProfile(String userId) {
        return MOCK_DB.getOrDefault(userId, new UserProfile(userId));
    }

    // 2. WRITE: The "Learning" Logic
    public void updateUserStats(String userId, double newOrderAmount, String newCuisine) {
        UserProfile user = getUserProfile(userId);

        // A. Dynamic Learning: Update Average Order Value
        double currentTotal = user.orderingBehavior.avgOrderValue * user.orderingBehavior.totalOrders;
        double newTotal = currentTotal + newOrderAmount;
        user.orderingBehavior.totalOrders++;
        user.orderingBehavior.avgOrderValue = newTotal / user.orderingBehavior.totalOrders;

        // B. Dynamic Learning: Add new cuisine if not present
        if (!user.preferences.cuisines.contains(newCuisine)) {
            user.preferences.cuisines.add(newCuisine);
        }

        // 🆕 C. Confidence Learning: Increase confidence for this cuisine
        double currentConf = user.preferences.cuisineConfidence.getOrDefault(newCuisine, 0.0);
        double newConf = Math.min(1.0, currentConf + 0.05); // +5% per order, capped at 1.0
        user.preferences.cuisineConfidence.put(newCuisine, newConf);

        // Save back to "DB"
        MOCK_DB.put(userId, user);
        System.out.println("🧠 UPDATED PROFILE: Avg Value=" + user.orderingBehavior.avgOrderValue
                + " | Cuisine '" + newCuisine + "' confidence=" + newConf);
    }

    // 🆕 3. UPDATE: Learn restaurant preference after ordering
    public void updateRestaurantPreference(String userId, String restaurantId) {
        UserProfile user = getUserProfile(userId);
        double currentPref = user.restaurantPreferences.getOrDefault(restaurantId, 0.5);
        double newPref = Math.min(1.0, currentPref + 0.05);
        user.restaurantPreferences.put(restaurantId, newPref);
        MOCK_DB.put(userId, user);
        System.out.println("🏪 UPDATED RESTAURANT PREF: " + restaurantId + " = " + newPref);
    }

    // 4. RESET: The "Forget Me" Logic
    public void clearUserProfile(String userId) {
        if (MOCK_DB.containsKey(userId)) {
            UserProfile cleanProfile = new UserProfile(userId);
            MOCK_DB.put(userId, cleanProfile);
            System.out.println("🧹 MEMORY WIPED for User: " + userId);
        }
    }
}