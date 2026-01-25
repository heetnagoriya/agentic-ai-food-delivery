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
        // Initialize your Tier-1 User (User 123)
        UserProfile u1 = new UserProfile("user_123");
        
        u1.preferences.cuisines.addAll(List.of("Italian", "Fast Food"));
        u1.preferences.likes.add("Spicy Pizza");
        u1.preferences.allergies.add("Peanuts");
        
        u1.budget.rangeMin = 400;
        u1.budget.rangeMax = 800;
        u1.budget.confidenceLevel = "high";
        
        u1.orderingBehavior.avgOrderValue = 650;
        u1.orderingBehavior.totalOrders = 10; // Assume they ordered 10 times before
        
        MOCK_DB.put("user_123", u1);
    }

    // 1. READ: Get the complex profile
    public UserProfile getUserProfile(String userId) {
        return MOCK_DB.getOrDefault(userId, new UserProfile(userId));
    }

    // 2. WRITE: The "Learning" Logic (Your specific request!)
    public void updateUserStats(String userId, double newOrderAmount, String newCuisine) {
        UserProfile user = getUserProfile(userId);

        // A. Dynamic Learning: Update Average Order Value
        // Formula: ((Old_Avg * Total) + New_Price) / (Total + 1)
        double currentTotal = user.orderingBehavior.avgOrderValue * user.orderingBehavior.totalOrders;
        double newTotal = currentTotal + newOrderAmount;
        user.orderingBehavior.totalOrders++; // Increment count
        user.orderingBehavior.avgOrderValue = newTotal / user.orderingBehavior.totalOrders;

        // B. Dynamic Learning: Add new cuisine if not present
        if (!user.preferences.cuisines.contains(newCuisine)) {
            user.preferences.cuisines.add(newCuisine);
        }

        // Save back to "DB"
        MOCK_DB.put(userId, user);
        System.out.println("🧠 UPDATED PROFILE: Avg Value is now " + user.orderingBehavior.avgOrderValue);
    }
}