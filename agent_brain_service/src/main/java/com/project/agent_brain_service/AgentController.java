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
import java.util.List;
import java.util.Map;

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

    // 1. THE ANALYST (Extracts Search Terms)
    private String extractSearchKeyword(String userQuestion) {
        try {
            String prompt = "Extract the FOOD ITEM or INTENT from: '" + userQuestion + "'. Output STRICTLY ONE WORD. If vague, output 'Top Rated'.";
            String jsonBody = "{ \"contents\": [{ \"parts\": [{\"text\": \"" + prompt + "\"}] }] }";
            String googleUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            String rawJson = restClient.post().uri(googleUrl).header("Content-Type", "application/json").body(jsonBody).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(rawJson);
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText().trim();
        } catch (Exception e) { return "Pizza"; }
    }

    @GetMapping("/ask")
    public AgentResponse askAgent(
            @RequestParam(value = "userId", defaultValue = "user_123") String userId,
            @RequestParam(value = "question", defaultValue = "I am hungry") String question) {
        
        try {
            // Context
            UserProfile profile = userProfileService.getUserProfile(userId);
            String userProfileJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
            String bestCoupon = couponService.getBestCoupon(); 
            
            // 🛡️ SAFELY CONVERT DISCOUNT TO STRING (Fixes the crash)
            double discountVal = couponService.getDiscountPercentage(bestCoupon) * 100;
            String discountStr = String.valueOf((int) discountVal); // "20"
            
            String lastOrderId = fakeSwiggyController.getLastOrderId();
            if (lastOrderId == null) lastOrderId = "None";

            // Search Logic
            String smartKeyword = extractSearchKeyword(question);
            if (smartKeyword.equalsIgnoreCase("Top Rated")) smartKeyword = ""; 
            List<Map<String, Object>> searchResults = fakeSwiggyController.searchFood(smartKeyword);
            String searchResultsJson = objectMapper.writeValueAsString(searchResults);

            // 🧠 2. THE ROBUST PROMPT (Uses %s for everything now)
            String systemPrompt = """
                You are 'Foodie-Bot', an intelligent agent.
                
                SEARCH: "%s" -> RESULT: %s
                USER PROFILE: %s
                COUPON: %s (%s%% OFF)
                LAST ORDER ID: %s
                
                DECISION RULES:
                1. ANALYZE: Can user afford it? If not, check if COUPON makes it affordable.
                2. BORDERLINE CONFIDENCE (60-80%%): If budget is tight, ASK confirmation.
                3. HIGH CONFIDENCE (>85%%): Auto-order if explicitly asked OR clear history match.
                
                RESPONSE RULES:
                - Explain WHY you made the decision.
                - Examples: "Price was 600, but I used a coupon to make it 480."
                
                OUTPUT JSON:
                {
                    "intent": "chat" OR "order" OR "track" OR "cancel",
                    "confidence": 0-100,
                    "reasoning": "Internal logic trace...",
                    "suggested_item": "Item Name",
                    "restaurant_id": "ID",
                    "coupon_code": "Code used",
                    "order_id": "ID",
                    "message": "Public response."
                }
                USER SAYS: 
                """.formatted(smartKeyword, searchResultsJson, userProfileJson, bestCoupon, discountStr, lastOrderId);

            String finalPrompt = systemPrompt + question;
            String jsonBody = """
                { "contents": [{ "parts": [{"text": "%s"}] }] }
                """.formatted(finalPrompt.replace("\n", "\\n").replace("\"", "\\\""));
            
            String googleUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            String rawJson = restClient.post().uri(googleUrl).header("Content-Type", "application/json").body(jsonBody).retrieve().body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            String cleanJson = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText().replace("```json", "").replace("```", "").trim();
            AgentResponse response = objectMapper.readValue(cleanJson, AgentResponse.class);

            // --- 🤖 EXECUTION ---
            if ("order".equalsIgnoreCase(response.intent) && response.suggestedItem != null) {
                String orderStatus = fakeSwiggyController.placeOrder(response.restaurantId, response.suggestedItem);
                
                MenuItem details = fakeSwiggyController.getItemDetails(response.restaurantId, response.suggestedItem);
                if (details != null) {
                    double paid = details.price;
                    if (response.couponCode != null) paid -= (paid * couponService.getDiscountPercentage(response.couponCode));
                    userProfileService.updateUserStats(userId, paid, details.cuisine);
                }
                
                if (response.confidence > 85) response.message = "🚀 [AUTO-PILOT ACTIVE] " + response.message + "\n\n" + orderStatus;
                else response.message = response.message + "\n\n" + orderStatus;
            }
            else if ("track".equalsIgnoreCase(response.intent)) {
                if (response.orderId != null) response.message += "\n" + fakeSwiggyController.getOrderStatus(response.orderId);
                else response.message = "I couldn't find that order ID.";
            } else if ("cancel".equalsIgnoreCase(response.intent)) {
                if (response.orderId != null) response.message += "\n" + fakeSwiggyController.cancelOrder(response.orderId);
                else response.message = "I couldn't find an order to cancel.";
            }

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            AgentResponse err = new AgentResponse();
            err.message = "🚨 Error: " + e.getMessage();
            return err;
        }
    }
}