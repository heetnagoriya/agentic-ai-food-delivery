package com.project.agent_brain_service;

import java.util.ArrayList; // Import added
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@NoArgsConstructor
@DynamoDbBean
public class MenuItem {
    public String name;
    public double price;
    public String cuisine;
    public List<String> tags;
    public List<Review> reviews = new ArrayList<>(); // 🆕 NEW FIELD
    public List<Customization> availableCustomizations = new ArrayList<>(); // 🆕 For Category B

    public MenuItem(String name, double price, String cuisine, List<String> tags) {
        this.name = name;
        this.price = price;
        this.cuisine = cuisine;
        this.tags = tags;
    }
    
    // 🆕 Helper to add reviews easily
    public void addReview(String user, double rating, String comment, String sentiment) {
        this.reviews.add(new Review(user, rating, comment, sentiment));
    }

    // 🆕 Helper to add customizations easily
    public void addCustomization(String name, double price, String type) {
        this.availableCustomizations.add(new Customization(name, price, type));
    }
}