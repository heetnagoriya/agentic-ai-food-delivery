package com.project.agent_brain_service;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@NoArgsConstructor
@DynamoDbBean
public class Restaurant {
    public String id;
    public String name;
    public boolean isOpen;
    public double rating;
    public String location;
    public List<MenuItem> menu = new ArrayList<>();
    public List<Coupon> coupons = new ArrayList<>();  // Each restaurant has its own coupons

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public Restaurant(String id, String name, boolean isOpen, double rating, String location) {
        this.id = id;
        this.name = name;
        this.isOpen = isOpen;
        this.rating = rating;
        this.location = location;
    }

    public void addItem(String name, double price, String cuisine, List<String> tags) {
        menu.add(new MenuItem(name, price, cuisine, tags));
    }

    public void addCoupon(String code, double percentOff, double flatOff, double maxDiscount, double minOrder, String description) {
        coupons.add(new Coupon(code, percentOff, flatOff, maxDiscount, minOrder, description));
    }
}