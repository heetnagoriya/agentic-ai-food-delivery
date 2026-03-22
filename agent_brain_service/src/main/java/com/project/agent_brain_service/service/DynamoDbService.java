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
    private DynamoDbTable<ChatSession> chatTable;

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
        chatTable = enhancedClient.table("FoodDelivery_ChatSessions", TableSchema.fromBean(ChatSession.class));

        createTableIfNotExists(userTable);
        createTableIfNotExists(orderTable);
        createTableIfNotExists(restaurantTable);
        createTableIfNotExists(cartTable);
        createTableIfNotExists(historyTable);
        createTableIfNotExists(walletTable);
        createTableIfNotExists(chatTable);
    }

    private void createTableIfNotExists(DynamoDbTable<?> table) {
        try {
            table.createTable();
            try (software.amazon.awssdk.services.dynamodb.DynamoDbClient standardClient =
                 software.amazon.awssdk.services.dynamodb.DynamoDbClient.create()) {

                software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter waiter =
                        standardClient.waiter();

                software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest request =
                        software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest.builder()
                        .tableName(table.tableName())
                        .build();

                System.out.println("Waiting for table " + table.tableName() + " to become ACTIVE...");
                waiter.waitUntilTableExists(request);
            }
        } catch (ResourceInUseException e) {
            // Table already exists — fine
        } catch (Exception e) {
            System.err.println("WARN: Could not create table: " + e.getMessage());
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

    // --- Chat Session Operations ---
    public void saveChat(ChatSession session) { chatTable.putItem(session); }
    public ChatSession getChat(String userId) { return chatTable.getItem(r -> r.key(k -> k.partitionValue(userId))); }
}
