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
        
        // Ensure these lists are initialized in UserProfile class, or we add checks
        if (u1.preferences != null) {
            u1.preferences.cuisines.addAll(List.of("Italian", "Fast Food"));
            u1.preferences.likes.add("Spicy Pizza");
            u1.preferences.allergies.add("Peanuts");
            
            u1.budget.rangeMin = 400;
            u1.budget.rangeMax = 800;
            u1.budget.confidenceLevel = "high";
            
            u1.orderingBehavior.avgOrderValue = 650;
            u1.orderingBehavior.totalOrders = 10;
        }
        
        MOCK_DB.put("user_123", u1);
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

        // Save back to "DB"
        MOCK_DB.put(userId, user);
        System.out.println("🧠 UPDATED PROFILE: Avg Value is now " + user.orderingBehavior.avgOrderValue);
    }

    // 3. RESET: The "Forget Me" Logic (Fixed Variable Names)
    public void clearUserProfile(String userId) {
        if (MOCK_DB.containsKey(userId)) {
            // Reset to default state (Create a fresh, empty profile)
            UserProfile cleanProfile = new UserProfile(userId);
            MOCK_DB.put(userId, cleanProfile);
            System.out.println("🧹 MEMORY WIPED for User: " + userId);
        }
    }
}