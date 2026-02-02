package com.project.agent_brain_service;

import java.util.ArrayList;
import java.util.List;

public class Restaurant {
    public String id;
    public String name;
    public boolean isOpen;
    public double rating; // 4.5
    public List<MenuItem> menu = new ArrayList<>();

    public Restaurant(String id, String name, boolean isOpen, double rating) {
        this.id = id;
        this.name = name;
        this.isOpen = isOpen;
        this.rating = rating;
    }

    public void addItem(String name, double price, String cuisine, List<String> tags) {
        menu.add(new MenuItem(name, price, cuisine, tags));
    }
}