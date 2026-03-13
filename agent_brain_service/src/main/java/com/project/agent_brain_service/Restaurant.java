package com.project.agent_brain_service;

import java.util.ArrayList;
import java.util.List;

public class Restaurant {
    public String id;
    public String name;
    public boolean isOpen;
    public double rating;
    public String location;
    public List<MenuItem> menu = new ArrayList<>();
    public List<Coupon> coupons = new ArrayList<>();  // Each restaurant has its own coupons

    public Restaurant(String id, String name, boolean isOpen, double rating, String location) {
        this.id = id;
        this.name = name;
        this.isOpen = isOpen;
        this.rating = rating;
        this.location = location;
    }

    public void addItem(String name, double price, String cuisine, List<String> tags, int stock) {
        menu.add(new MenuItem(name, price, cuisine, tags, stock));
    }

    public void addCoupon(String code, double percentOff, double flatOff, double maxDiscount, double minOrder, String description) {
        coupons.add(new Coupon(code, percentOff, flatOff, maxDiscount, minOrder, description));
    }
}