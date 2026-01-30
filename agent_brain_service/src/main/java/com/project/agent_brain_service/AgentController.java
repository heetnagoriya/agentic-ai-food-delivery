package com.project.agent_brain_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.agent_brain_service.controller.FakeSwiggyController; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class AgentController {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Autowired
    private UserProfileService userProfileService;
    
    @Autowired
    private FakeSwiggyController fakeSwiggyController; 
    
    @Autowired
    private CouponService couponService; 

    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/ask")
    public AgentResponse askAgent(
            @RequestParam(value = "userId", defaultValue = "user_123") String userId,
            @RequestParam(value = "question", defaultValue = "I am hungry") String question) {
        
        try {
            // 1. Gather Context
            UserProfile profile = userProfileService.getUserProfile(userId);
            String userProfileJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
            String validMenu = String.join(", ", FakeSwiggyController.getMenuItems());
            
            // Get Coupon Info
            String bestCoupon = couponService.getBestCoupon(); 
            double discount = couponService.getDiscountPercentage(bestCoupon) * 100;

            // 2. 🧠 SYSTEM PROMPT (FIXED THE % BUG HERE)
            String systemPrompt = """
                You are 'Foodie-Bot'. 
                
                CONTEXT:
                - Valid Menu: [%s]
                - Available Coupon: Code '%s' gives %.0f%% OFF.
                - User Profile: %s
                
                YOUR JOB (STEP-BY-STEP REASONING):
                1. Identify what the user likely wants.
                2. Check the price (Pizza is 600, Pasta is 450).
                3. Compare Price vs User's Budget.
                4. CRITICAL: If Price > Budget, APPLY the Coupon math (Price - 20%%).
                5. If (Price - Coupon) < Budget, then you CAN order it.
                
                CONFIDENCE SCORING:
                - If within budget (or made affordable by coupon) + User Likes it -> Score 90+.
                - If still too expensive -> Score 0-10.
                
                OUTPUT JSON:
                {
                    "intent": "chat" OR "order",
                    "confidence": 0-100,
                    "reasoning": "Show math. E.g. '600 is too high, but with 20%% off it becomes 480'",
                    "suggested_item": "Item Name",
                    "coupon_code": "Code used or null",
                    "final_price": "Calculated price",
                    "message": "..."
                }
                USER SAYS: 
                """.formatted(validMenu, bestCoupon, discount, userProfileJson);

            // 3. Call Gemini
            String finalPrompt = systemPrompt + question;
            String jsonBody = """
                { "contents": [{ "parts": [{"text": "%s"}] }] }
                """.formatted(finalPrompt.replace("\n", "\\n").replace("\"", "\\\""));
                
            String googleUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            String rawJson = restClient.post().uri(googleUrl).header("Content-Type", "application/json").body(jsonBody).retrieve().body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            String aiText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            String cleanJson = aiText.replace("```json", "").replace("```", "").trim();
            AgentResponse response = objectMapper.readValue(cleanJson, AgentResponse.class);

            // --- 🤖 EXECUTION LOGIC ---
            if ("order".equalsIgnoreCase(response.intent) && response.suggestedItem != null) {
                
                String orderStatus = fakeSwiggyController.placeOrder(response.suggestedItem);
                
                MenuItem itemDetails = fakeSwiggyController.getItemDetails(response.suggestedItem);
                if (itemDetails != null) {
                    double paidAmount = itemDetails.price; 
                    
                    // Apply coupon discount if the AI used one
                    if (response.couponCode != null && !response.couponCode.equals("null")) {
                        paidAmount = paidAmount - (paidAmount * couponService.getDiscountPercentage(response.couponCode));
                    }
                    
                    userProfileService.updateUserStats(userId, paidAmount, itemDetails.cuisine);
                }

                if (response.confidence > 85) {
                    response.message = "[AUTO-PILOT 🤖] Used Coupon " + response.couponCode + "! " + response.message + " " + orderStatus;
                } else {
                    response.message = response.message + " " + orderStatus;
                }
            }
            
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            AgentResponse error = new AgentResponse();
            error.message = "🚨 ERROR: " + e.getMessage();
            return error;
        }
    }
}