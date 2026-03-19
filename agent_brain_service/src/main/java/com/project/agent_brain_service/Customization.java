package com.project.agent_brain_service;

/**
 * Represents a customization option for a menu item.
 * Examples: "Extra Cheese +₹50", "No Onions +₹0", "Mushroom Topping +₹40"
 */

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@NoArgsConstructor
@DynamoDbBean
public class Customization {
    public String name;           // e.g., "Extra Cheese"
    public double additionalPrice; // e.g., 50.0 (₹0 for removals)
    public String type;           // "ADD" or "REMOVE"

    public Customization(String name, double additionalPrice, String type) {
        this.name = name;
        this.additionalPrice = additionalPrice;
        this.type = type;
    }

    @Override
    public String toString() {
        if (type.equals("REMOVE")) return name + " (free)";
        return name + (additionalPrice > 0 ? " (+₹" + additionalPrice + ")" : "");
    }
}
