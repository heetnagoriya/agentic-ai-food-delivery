package com.project.agent_brain_service;

import java.util.ArrayList; // Import added
import java.util.List;

public class MenuItem {
    public String name;
    public double price;
    public String cuisine;
    public List<String> tags;
    public int stockCount;
    public List<Review> reviews = new ArrayList<>(); // 🆕 NEW FIELD

    public MenuItem(String name, double price, String cuisine, List<String> tags, int stockCount) {
        this.name = name;
        this.price = price;
        this.cuisine = cuisine;
        this.tags = tags;
        this.stockCount = stockCount;
    }
    
    // 🆕 Helper to add reviews easily
    public void addReview(String user, double rating, String comment, String sentiment) {
        this.reviews.add(new Review(user, rating, comment, sentiment));
    }
}