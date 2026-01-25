package com.project.agent_brain_service;

import java.util.List;
import java.util.ArrayList;

// 1. The Main Profile Class
public class UserProfile {
    public String userId;
    public Preferences preferences;
    public Budget budget;
    public OrderingBehavior orderingBehavior;

    // Constructor to initialize a blank user (or load from DB later)
    public UserProfile(String userId) {
        this.userId = userId;
        this.preferences = new Preferences();
        this.budget = new Budget();
        this.orderingBehavior = new OrderingBehavior();
    }

    // --- NESTED CLASSES (Your JSON Structure) ---

    public static class Preferences {
        public List<String> cuisines = new ArrayList<>();
        public List<String> likes = new ArrayList<>();
        public List<String> allergies = new ArrayList<>();
    }

    public static class Budget {
        public double rangeMin = 0;
        public double rangeMax = 0;
        public String currency = "INR";
        public String confidenceLevel = "low"; // 'low', 'medium', 'high'
    }

    public static class OrderingBehavior {
        public double avgOrderValue = 0;
        public int totalOrders = 0; // Needed to calculate new average!
        public String discountSensitivity = "medium";
    }
}