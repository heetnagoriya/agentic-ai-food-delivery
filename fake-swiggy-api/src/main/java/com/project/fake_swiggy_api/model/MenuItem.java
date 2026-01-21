package com.project.fake_swiggy_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class MenuItem {
    private String id;
    private String restaurantId; // LINK to the Restaurant
    private String name;
    private Double price;
    private String tags; 
    private boolean isVeg;

    @DynamoDbPartitionKey
    public String getId() { return id; }
}