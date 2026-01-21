package com.project.fake_swiggy_api.service;

import com.project.fake_swiggy_api.model.Coupon;
import com.project.fake_swiggy_api.model.MenuItem;
import com.project.fake_swiggy_api.model.Restaurant;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.UUID;

@Service
public class DatabaseInitializer {

    private final DynamoDbTable<Restaurant> restaurantTable;
    private final DynamoDbTable<MenuItem> menuTable;

    public DatabaseInitializer(DynamoDbEnhancedClient enhancedClient) {
        this.restaurantTable = enhancedClient.table("fakecom.project.fake_swiggy_api_Restaurants", TableSchema.fromBean(Restaurant.class));
        this.menuTable = enhancedClient.table("fakecom.project.fake_swiggy_api_Menu", TableSchema.fromBean(MenuItem.class));
    }

    @PostConstruct
    public void init() {
        createTables();
        seedData();
    }

    private void createTables() {
        try {
            restaurantTable.createTable();
            menuTable.createTable();
            System.out.println("✅ AWS DynamoDB Tables Created!");
        } catch (Exception e) {
            System.out.println("ℹ️ Tables already exist.");
        }
    }

    private void seedData() {
        // Check if data exists. If yes, don't add duplicates.
        if (restaurantTable.scan().items().iterator().hasNext()) {
            return; 
        }

        System.out.println("🚀 Seeding Dummy Data...");

        // 1. Create Domino's
        Restaurant r1 = new Restaurant();
        r1.setId("rest_01");
        r1.setName("Domino's Pizza");
        r1.setLocation("Anand");
        r1.setDeliveryTimeMins(30);
        r1.setCoupons(List.of(
            new Coupon("DOM50", 50, 100), // 50% off upto 100
            new Coupon("FREEDEL", 0, 0)
        ));
        restaurantTable.putItem(r1);

        // 2. Create Items for Domino's
        menuTable.putItem(new MenuItem(UUID.randomUUID().toString(), "rest_01", "Margherita", 250.0, "Pizza,Veg,Italian", true));
        menuTable.putItem(new MenuItem(UUID.randomUUID().toString(), "rest_01", "Pepperoni Feast", 450.0, "Pizza,Non-Veg,Italian", false));

        // 3. Create La Pino'z
        Restaurant r2 = new Restaurant();
        r2.setId("rest_02");
        r2.setName("La Pino'z Pizza");
        r2.setLocation("Anand");
        r2.setDeliveryTimeMins(45); // Slower
        r2.setCoupons(List.of(new Coupon("BOGO", 100, 300))); // Buy 1 Get 1 (Simulated)
        restaurantTable.putItem(r2);

        menuTable.putItem(new MenuItem(UUID.randomUUID().toString(), "rest_02", "7 Cheese Pizza", 600.0, "Pizza,Veg,Cheesy", true));

        System.out.println("✅ Data Seeding Complete!");
    }
}