package com.project.agent_brain_service;

import java.util.ArrayList;
import java.util.List;

/**
 * Shopping cart for a user. Supports multi-item, multi-restaurant orders.
 */
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@NoArgsConstructor
@DynamoDbBean
public class Cart {
    public String userId;
    public List<CartItem> items = new ArrayList<>();

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }

    public Cart(String userId) {
        this.userId = userId;
    }

    public void addItem(CartItem item) {
        // Check if same item from same restaurant exists — merge quantities
        for (CartItem existing : items) {
            if (existing.restaurantId.equals(item.restaurantId)
                    && existing.itemName.equalsIgnoreCase(item.itemName)
                    && existing.customizations.equals(item.customizations)) {
                existing.quantity += item.quantity;
                return;
            }
        }
        items.add(item);
    }

    public boolean removeItem(String itemName, String restaurantId) {
        return items.removeIf(i ->
                i.itemName.equalsIgnoreCase(itemName)
                && (restaurantId == null || restaurantId.isEmpty() || i.restaurantId.equals(restaurantId)));
    }

    public double getTotal() {
        return items.stream().mapToDouble(CartItem::getLineTotal).sum();
    }

    public int getItemCount() {
        return items.stream().mapToInt(i -> i.quantity).sum();
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
