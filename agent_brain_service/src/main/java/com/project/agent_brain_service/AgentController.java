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
            @RequestParam(value = "question", defaultValue = "I am hungry") String question,
            @RequestParam(value = "autoPilot", defaultValue = "true") boolean autoPilot) {
        
        try {
            // --- CONTEXT GATHERING (THE SENSORS) ---
            UserProfile profile = userProfileService.getUserProfile(userId);
            String userProfileJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
            
            // 🆕 SENSOR 1: MONEY
            double currentBalance = fakeSwiggyController.getWalletBalance(userId);
            
            // 🆕 SENSOR 2: INVENTORY (Via Search)
            String smartKeyword = extractSearchKeyword(question);
            if (smartKeyword.equalsIgnoreCase("Top Rated")) smartKeyword = ""; 
            List<Map<String, Object>> searchResults = fakeSwiggyController.searchFood(smartKeyword);
            String searchResultsJson = objectMapper.writeValueAsString(searchResults);

            // Discounts
            String bestCoupon = couponService.getBestCoupon(); 
            double discountVal = couponService.getDiscountPercentage(bestCoupon) * 100;
            String discountStr = String.valueOf((int) discountVal);
            String lastOrderId = fakeSwiggyController.getLastOrderId();
            if (lastOrderId == null) lastOrderId = "None";

            // 🧠 2. THE MNC-GRADE SYSTEM PROMPT
            String systemPrompt = """
                You are 'Foodie-Bot', an Autonomous Agent managing a real wallet.
                
                --- 🌍 WORLD STATE ---
                💰 USER WALLET: ₹%.2f
                🔍 SEARCH RESULTS: %s
                👤 USER PROFILE: %s
                🎟️ COUPON: %s (%s%% OFF)
                ----------------------
                
                --- ⚙️ DECISION LOGIC (CONSTRAINT SATISFACTION) ---
                1. 🛑 HARD CONSTRAINT (STOCK): If 'stock' is 0, item is UNSELLABLE. Suggest alternatives.
                2. 🛑 HARD CONSTRAINT (MONEY): 
                   - Calculate Final Price = (Price - Coupon).
                   - If Final Price > WALLET BALANCE, you CANNOT order. 
                   - Reply: "Payment would be declined. You only have ₹[Balance]."
                3. ⚠️ SOFT CONSTRAINT (BUDGET PREFERENCE):
                   - If Price is within Wallet but above User's preferred range, ASK before ordering.
                4. ✅ EXECUTION:
                   - Only 'order' if Stock > 0 AND Wallet > Price.
                
                OUTPUT JSON:
                {
                    "intent": "chat" OR "order" OR "track" OR "cancel",
                    "confidence": 0-100,
                    "reasoning": "Step-by-step math: Price 600 - Coupon 120 = 480. Wallet has 100. 480 > 100. Fail.",
                    "suggested_item": "Item Name",
                    "restaurant_id": "ID",
                    "coupon_code": "Code used (or null)",
                    "order_id": "ID",
                    "message": "Public response."
                }
                USER SAYS: 
                """.formatted(currentBalance, searchResultsJson, userProfileJson, bestCoupon, discountStr);

            String finalPrompt = systemPrompt + question;
            String jsonBody = """
                { "contents": [{ "parts": [{"text": "%s"}] }] }
                """.formatted(finalPrompt.replace("\n", "\\n").replace("\"", "\\\""));
            
            String googleUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            String rawJson = restClient.post().uri(googleUrl).header("Content-Type", "application/json").body(jsonBody).retrieve().body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            String cleanJson = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText().replace("```json", "").replace("```", "").trim();
            AgentResponse response = objectMapper.readValue(cleanJson, AgentResponse.class);

            // --- 🤖 EXECUTION LAYER ---
            if ("order".equalsIgnoreCase(response.intent) && response.suggestedItem != null) {
                
                // 🛑 SAFETY CHECK: Only execute if Confidence is High AND Auto-Pilot is ON
                if (response.confidence > 85 && autoPilot) {
                    
                    // 1. Calculate the Real Price to Charge (Discount Logic)
                    MenuItem details = fakeSwiggyController.getItemDetails(response.restaurantId, response.suggestedItem);
                    double finalPrice = details.price;
                    
                    if (response.couponCode != null && !response.couponCode.isEmpty()) {
                        double discount = couponService.getDiscountPercentage(response.couponCode);
                        finalPrice = finalPrice - (finalPrice * discount);
                    }

                    // 2. Place Order with FINAL PRICE
                    String orderStatus = fakeSwiggyController.placeOrder(
                        response.restaurantId, 
                        response.suggestedItem, 
                        userId, 
                        finalPrice // 🆕 Sending the discounted amount to Backend
                    );
                    
                    // 3. Handle Success vs Failure
                    if (orderStatus.contains("DECLINED") || orderStatus.contains("OUT OF STOCK")) {
                        response.message = "⚠️ TRANSACTION FAILED: " + orderStatus;
                    } else {
                        response.message = "🚀 [AUTO-PILOT] Transaction Successful!\n\n" + orderStatus;
                        // Learning update
                        userProfileService.updateUserStats(userId, finalPrice, details.cuisine);
                    }
                } else {
                    // ✋ HUMAN-IN-THE-LOOP (Draft Only)
                    response.message = "🛡️ [SAFETY MODE] " + response.message + 
                                       "\n\n❌ Order NOT placed yet. Type 'Yes' or 'Confirm' to proceed.";
                }
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