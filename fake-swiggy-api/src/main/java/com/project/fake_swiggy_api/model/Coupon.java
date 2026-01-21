package com.project.fake_swiggy_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean // This allows it to be stored inside the Restaurant table
public class Coupon {
    private String code;        // e.g., "SWIGGY50"
    private int discountPercent; // e.g., 50
    private int maxDiscountAmount; // e.g., 100
}