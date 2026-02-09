package com.project.agent_brain_service;

import java.util.List;

public class MenuItem {
    public String name;
    public double price;
    public String cuisine;
    public List<String> tags;
    public int stockCount; // 🆕 REAL STOCK TRACKING

    public MenuItem(String name, double price, String cuisine, List<String> tags, int stockCount) {
        this.name = name;
        this.price = price;
        this.cuisine = cuisine;
        this.tags = tags;
        this.stockCount = stockCount;
    }
}