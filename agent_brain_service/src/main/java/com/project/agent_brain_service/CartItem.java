package com.project.agent_brain_service;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single item in the user's cart.
 */
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@NoArgsConstructor
@DynamoDbBean
public class CartItem {
    public String restaurantId;
    public String restaurantName;
    public String itemName;
    public double unitPrice;
    public int quantity;
    public List<String> customizations; // e.g., ["Extra Cheese", "No Onions"]
    public double customizationTotal;   // Total extra cost from customizations

    public CartItem(String restaurantId, String restaurantName, String itemName,
                    double unitPrice, int quantity, List<String> customizations, double customizationTotal) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.itemName = itemName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.customizations = customizations != null ? customizations : new ArrayList<>();
        this.customizationTotal = customizationTotal;
    }

    public double getLineTotal() {
        return (unitPrice + customizationTotal) * quantity;
    }
}
