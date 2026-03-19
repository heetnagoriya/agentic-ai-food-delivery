package com.project.agent_brain_service.service;

import com.project.agent_brain_service.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DynamoDbService {
    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<UserProfile> userTable;
    private DynamoDbTable<Order> orderTable;
    private DynamoDbTable<Restaurant> restaurantTable;
    private DynamoDbTable<Cart> cartTable;
    private DynamoDbTable<UserOrderHistory> historyTable;
    private DynamoDbTable<UserWallet> walletTable;

    public DynamoDbService(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    @PostConstruct
    public void init() {
        userTable = enhancedClient.table("FoodDelivery_Users", TableSchema.fromBean(UserProfile.class));
        orderTable = enhancedClient.table("FoodDelivery_Orders", TableSchema.fromBean(Order.class));
        restaurantTable = enhancedClient.table("FoodDelivery_Restaurants", TableSchema.fromBean(Restaurant.class));
        cartTable = enhancedClient.table("FoodDelivery_Carts", TableSchema.fromBean(Cart.class));
        historyTable = enhancedClient.table("FoodDelivery_History", TableSchema.fromBean(UserOrderHistory.class));
        walletTable = enhancedClient.table("FoodDelivery_Wallets", TableSchema.fromBean(UserWallet.class));

        createTableIfNotExists(userTable);
        createTableIfNotExists(orderTable);
        createTableIfNotExists(restaurantTable);
        createTableIfNotExists(cartTable);
        createTableIfNotExists(historyTable);
        createTableIfNotExists(walletTable);
    }

    private void createTableIfNotExists(DynamoDbTable<?> table) {
        try {
            table.createTable();
        } catch (ResourceInUseException e) {
            // Table already exists
        } catch (Exception e) {
            System.err.println("WARN: Could not create table (might already exist or missing credentials): " + e.getMessage());
        }
    }

    // --- User Operations ---
    public void saveUser(UserProfile user) { userTable.putItem(user); }
    public UserProfile getUser(String userId) { return userTable.getItem(r -> r.key(k -> k.partitionValue(userId))); }

    // --- Order Operations ---
    public void saveOrder(Order order) { orderTable.putItem(order); }
    public Order getOrder(String orderId) { return orderTable.getItem(r -> r.key(k -> k.partitionValue(orderId))); }
    public List<Order> getAllOrders() { return orderTable.scan().items().stream().collect(Collectors.toList()); }

    // --- Cart Operations ---
    public void saveCart(Cart cart) { cartTable.putItem(cart); }
    public Cart getCart(String userId) { return cartTable.getItem(r -> r.key(k -> k.partitionValue(userId))); }

    // --- Wallet Operations ---
    public void saveWallet(UserWallet wallet) { walletTable.putItem(wallet); }
    public UserWallet getWallet(String userId) { return walletTable.getItem(r -> r.key(k -> k.partitionValue(userId))); }

    // --- History Operations ---
    public void saveHistory(UserOrderHistory history) { historyTable.putItem(history); }
    public UserOrderHistory getHistory(String userId) { return historyTable.getItem(r -> r.key(k -> k.partitionValue(userId))); }

    // --- Restaurant Operations ---
    public void saveRestaurant(Restaurant restaurant) { restaurantTable.putItem(restaurant); }
    public Restaurant getRestaurant(String id) { return restaurantTable.getItem(r -> r.key(k -> k.partitionValue(id))); }
    public List<Restaurant> getAllRestaurants() { return restaurantTable.scan().items().stream().collect(Collectors.toList()); }
}
