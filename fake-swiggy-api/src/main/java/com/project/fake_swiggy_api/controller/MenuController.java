package com.project.fake_swiggy_api.controller;

import com.project.fake_swiggy_api.model.MenuItem;
import com.project.fake_swiggy_api.model.Restaurant;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class MenuController {

    private final DynamoDbEnhancedClient enhancedClient;

    public MenuController(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    // 1. Get All Restaurants (The Agent will look here first)
    @GetMapping("/restaurants")
    public List<Restaurant> getRestaurants() {
        List<Restaurant> list = new ArrayList<>();
        enhancedClient.table("fakecom.project.fake_swiggy_api_Restaurants", TableSchema.fromBean(Restaurant.class))
                      .scan().items().iterator().forEachRemaining(list::add);
        return list;
    }

    // 2. Get Menu for a specific Restaurant
    @GetMapping("/restaurant/{id}/menu")
    public List<MenuItem> getMenuByRestaurant(@PathVariable String id) {
        List<MenuItem> allItems = new ArrayList<>();
        enhancedClient.table("fakecom.project.fake_swiggy_api_Menu", TableSchema.fromBean(MenuItem.class))
                      .scan().items().iterator().forEachRemaining(allItems::add);
        
        // Filter in memory (Simple for now)
        return allItems.stream()
                .filter(item -> item.getRestaurantId().equals(id))
                .collect(Collectors.toList());
    }
}