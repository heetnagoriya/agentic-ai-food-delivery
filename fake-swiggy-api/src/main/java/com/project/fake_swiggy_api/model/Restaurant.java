package com.project.fake_swiggy_api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import java.util.List;

@Data
@NoArgsConstructor
@DynamoDbBean
public class Restaurant {
    private String id;
    private String name;            // e.g., "La Pino'z Pizza"
    private String location;        // e.g., "Anand, Gujarat"
    private int deliveryTimeMins;   // e.g., 35
    private List<Coupon> coupons;   // e.g., SWIGGY50

    @DynamoDbPartitionKey
    public String getId() { return id; }
}