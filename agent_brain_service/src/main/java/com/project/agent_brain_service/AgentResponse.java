package com.project.agent_brain_service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentResponse {
    public String intent;
    public String reasoning;
    
    @JsonProperty("suggested_item")
    public String suggestedItem;
    
    @JsonProperty("coupon_code")
    public String couponCode;    // 🆕 The Agent tells us which coupon it used
    
    @JsonProperty("final_price")
    public double finalPrice;    // 🆕 The price after math
    
    public String message;
    public int confidence; 
}