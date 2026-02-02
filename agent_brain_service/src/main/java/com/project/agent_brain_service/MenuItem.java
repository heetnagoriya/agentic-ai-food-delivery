package com.project.agent_brain_service;

import java.util.List;

public class MenuItem {
    public String name;
    public double price;
    public String cuisine;
    public List<String> tags; // 🆕 ["Light", "Spicy", "Veg"]

    public MenuItem(String name, double price, String cuisine, List<String> tags) {
        this.name = name;
        this.price = price;
        this.cuisine = cuisine;
        this.tags = tags;
    }
}