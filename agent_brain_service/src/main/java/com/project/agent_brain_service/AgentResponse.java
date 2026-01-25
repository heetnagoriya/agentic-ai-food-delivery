package com.project.agent_brain_service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentResponse {
    public String intent;           // "chat" or "order"
    public String reasoning;        // The "Brain's" logic
    
    @JsonProperty("suggested_item")
    public String suggestedItem;    // "Spicy Pizza"
    
    public String message;          // "Ordering now!"
}