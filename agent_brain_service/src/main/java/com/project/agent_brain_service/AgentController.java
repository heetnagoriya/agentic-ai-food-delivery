package com.project.agent_brain_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// ✅ IMPORT THE CONTROLLER CORRECTLY
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

    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/ask")
    public AgentResponse askAgent(
            @RequestParam(value = "userId", defaultValue = "user_123") String userId,
            @RequestParam(value = "question", defaultValue = "I am hungry") String question) {
        
        try {
            // 1. Get Context
            UserProfile profile = userProfileService.getUserProfile(userId);
            String userProfileJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
            String validMenu = String.join(", ", FakeSwiggyController.getMenuItems());

            // 2. 🧠 THE CONFIDENCE PROMPT
            String systemPrompt = """
                You are 'Foodie-Bot'.
                VALID MENU: [%s]
                USER DATA: %s
                
                CONFIDENCE RULES:
                - Explicit "Order X" -> Confidence 100.
                - "I'm hungry" + Clear History -> Confidence 85-95.
                - Vague request -> Confidence 50.
                
                DECISION RULES:
                1. Confidence > 85 -> Set "intent" to "order" (AUTO-PILOT).
                2. Confidence < 85 -> Set "intent" to "chat".
                
                OUTPUT JSON:
                {
                    "intent": "chat" OR "order",
                    "confidence": 0-100,
                    "reasoning": "...",
                    "suggested_item": "Exact Menu Item Name",
                    "message": "..."
                }
                USER SAYS: 
                """.formatted(validMenu, userProfileJson);

            // 3. Call Gemini
            String finalPrompt = systemPrompt + question;
            String jsonBody = """
                { "contents": [{ "parts": [{"text": "%s"}] }] }
                """.formatted(finalPrompt.replace("\n", "\\n").replace("\"", "\\\""));
                
            String googleUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            String rawJson = restClient.post().uri(googleUrl).header("Content-Type", "application/json").body(jsonBody).retrieve().body(String.class);

            // 4. Parse Response
            JsonNode root = objectMapper.readTree(rawJson);
            String aiText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            String cleanJson = aiText.replace("```json", "").replace("```", "").trim();
            AgentResponse response = objectMapper.readValue(cleanJson, AgentResponse.class);

            // --- 🤖 EXECUTION & LEARNING LAYER 🤖 ---
            if ("order".equalsIgnoreCase(response.intent) && response.suggestedItem != null) {
                
                // A. Execute Order
                String orderStatus = fakeSwiggyController.placeOrder(response.suggestedItem);
                
                // B. Fetch REAL Data for Learning (This is what caused the error before!)
                MenuItem itemDetails = fakeSwiggyController.getItemDetails(response.suggestedItem);
                
                if (itemDetails != null) {
                    // C. Update Profile with REAL Price and Cuisine
                    // (Assuming $1 = 80 INR for this simulation)
                    double paidAmountINR = itemDetails.price * 80;
                    userProfileService.updateUserStats(userId, paidAmountINR, itemDetails.cuisine);
                    
                    System.out.println("🧠 LEARNED: User ate " + itemDetails.cuisine + " for " + paidAmountINR + " INR");
                }

                // D. Message Handling
                if (response.confidence > 85 && !question.toLowerCase().contains("order")) {
                    response.message = "[AUTO-PILOT 🤖] Confidence " + response.confidence + "%. " + response.message + " " + orderStatus;
                } else {
                    response.message = response.message + " " + orderStatus;
                }
            }
            
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            AgentResponse errorResponse = new AgentResponse();
            errorResponse.message = "Error: " + e.getMessage();
            return errorResponse;
        }
    }
}