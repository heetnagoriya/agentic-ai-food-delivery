package com.project.fake_swiggy_api.service;

import com.project.fake_swiggy_api.model.MenuItem;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

@Service
public class MenuService {

    private final DynamoDbTable<MenuItem> menuTable;

    public MenuService(DynamoDbEnhancedClient enhancedClient) {
        // Map the class to the table name "FakeSwiggy_Menu"
        this.menuTable = enhancedClient.table("FakeSwiggy_Menu", TableSchema.fromBean(MenuItem.class));
    }

    @PostConstruct // Runs automatically on startup
    public void createTableIfNotExists() {
        try {
            menuTable.createTable(); // Tries to create the table
            System.out.println("✅ AWS DynamoDB Table 'FakeSwiggy_Menu' created successfully!");
        } catch (Exception e) {
            System.out.println("ℹ️ Table likely already exists. Skipping creation.");
        }
    }

    // Method to Add Food
    public void addMenuItem(MenuItem item) {
        menuTable.putItem(item);
    }

    // Method to Get All Food (Simple Scan)
    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        Iterator<MenuItem> results = menuTable.scan().items().iterator();
        results.forEachRemaining(items::add);
        return items;
    }
}