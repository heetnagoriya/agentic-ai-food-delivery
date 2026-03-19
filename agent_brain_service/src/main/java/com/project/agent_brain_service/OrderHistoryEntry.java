package com.project.agent_brain_service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Records a past order for order history, recommendations, and reorder features.
 */
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@NoArgsConstructor
@DynamoDbBean
public class OrderHistoryEntry {
    public String orderId;
    public String itemName;
    public String restaurantId;
    public String restaurantName;
    public double price;
    public String couponCode;
    public List<String> customizations;
    public LocalDateTime orderDate;
    public String cuisine;

    public OrderHistoryEntry(String orderId, String itemName, String restaurantId,
                             String restaurantName, double price, String couponCode,
                             List<String> customizations, String cuisine) {
        this.orderId = orderId;
        this.itemName = itemName;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.price = price;
        this.couponCode = couponCode;
        this.customizations = customizations;
        this.cuisine = cuisine;
        this.orderDate = LocalDateTime.now();
    }
}
