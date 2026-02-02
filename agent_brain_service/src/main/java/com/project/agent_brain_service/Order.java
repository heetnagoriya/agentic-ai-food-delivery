package com.project.agent_brain_service;

import java.time.LocalDateTime;
import java.util.UUID;

public class Order {
    public String orderId;
    public String item;
    public String status; // "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"
    public LocalDateTime orderTime;
    public int deliveryTimeMins;

    public Order(String item) {
        this.orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        this.item = item;
        this.status = "PREPARING";
        this.orderTime = LocalDateTime.now();
        this.deliveryTimeMins = 30; // Default 30 mins
    }
}