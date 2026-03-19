package com.project.agent_brain_service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

// 1. The Main Profile Class
@Data
@NoArgsConstructor
@DynamoDbBean
public class UserProfile {
    public String userId;
    public Preferences preferences;
    public Budget budget;
    public OrderingBehavior orderingBehavior;

    // 🆕 Controlled Autonomy: FULL_AUTO / BALANCED / CONSERVATIVE
    public String autonomyLevel = "BALANCED";

    // 🆕 Language preference for multi-language support (English, Hindi, Gujarati)
    public String languagePreference = "English";

    // 🆕 Restaurant-level preference scores (e.g., {"res_1": 0.95, "res_3": 0.7})
    public Map<String, Double> restaurantPreferences = new HashMap<>();

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }

    // Constructor to initialize a blank user (or load from DB later)
    public UserProfile(String userId) {
        this.userId = userId;
        this.preferences = new Preferences();
        this.budget = new Budget();
        this.orderingBehavior = new OrderingBehavior();
    }

    // --- NESTED CLASSES (Your JSON Structure) ---

    @Data
    @NoArgsConstructor
    @DynamoDbBean
    public static class Preferences {
        public List<String> cuisines = new ArrayList<>();
        public List<String> likes = new ArrayList<>();
        public List<String> allergies = new ArrayList<>();

        // 🆕 Feature 1: Dislikes & Restaurant Blacklist
        public List<String> dislikes = new ArrayList<>();
        public List<String> blacklistedRestaurants = new ArrayList<>();

        // 🆕 Feature 2: Per-cuisine confidence (e.g., {"Italian": 0.9, "Fast Food": 0.8})
        public Map<String, Double> cuisineConfidence = new HashMap<>();
    }

    @Data
    @NoArgsConstructor
    @DynamoDbBean
    public static class Budget {
        public double rangeMin = 0;
        public double rangeMax = 0;
        public String currency = "INR";
        public String confidenceLevel = "low"; // 'low', 'medium', 'high'
    }

    @Data
    @NoArgsConstructor
    @DynamoDbBean
    public static class OrderingBehavior {
        public double avgOrderValue = 0;
        public int totalOrders = 0; // Needed to calculate new average!
        public String discountSensitivity = "medium";
    }
}