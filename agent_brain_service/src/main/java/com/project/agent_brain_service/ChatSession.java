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
public class ChatSession {
    public String userId;
    public List<ChatMessage> messages = new ArrayList<>();

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }

    public ChatSession(String userId) {
        this.userId = userId;
    }

    @Data
    @NoArgsConstructor
    @DynamoDbBean
    public static class ChatMessage {
        public long id;
        public String role;
        public String text;
        public String time;
        public double confidence;
        public List<AgentResponse.TraceStep> trace = new ArrayList<>();

        public ChatMessage(long id, String role, String text, String time, double confidence, List<AgentResponse.TraceStep> trace) {
            this.id = id;
            this.role = role;
            this.text = text;
            this.time = time;
            this.confidence = confidence;
            this.trace = trace != null ? trace : new ArrayList<>();
        }
    }
}
