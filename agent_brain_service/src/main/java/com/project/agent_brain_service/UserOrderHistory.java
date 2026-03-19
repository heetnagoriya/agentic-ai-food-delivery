package com.project.agent_brain_service;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@NoArgsConstructor
@DynamoDbBean
public class UserOrderHistory {
    public String userId;
    public List<OrderHistoryEntry> history = new ArrayList<>();

    public UserOrderHistory(String userId) {
        this.userId = userId;
    }

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }
}
