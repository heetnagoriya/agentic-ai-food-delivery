package com.project.agent_brain_service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentResponse {
    public String intent;           // "chat" or "order"
    public String reasoning;
    
    @JsonProperty("suggested_item")
    public String suggestedItem;
    
    public String message;
    
    // 🆕 NEW FIELD: The Confidence Score (0-100)
    public int confidence; 
}