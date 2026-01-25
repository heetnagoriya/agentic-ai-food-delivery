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
    
    // Injecting the "Body" (Fake Swiggy) into the "Brain"
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

            // 2. Build Prompt
            String systemPrompt = """
                You are 'Foodie-Bot'.
                CURRENT USER DATA (JSON): %s
                
                RULES:
                1. If user confirms an order (e.g., "Yes", "Order it"), set "intent" to "order".
                2. If "intent" is "order", you MUST output the exact food name in "suggested_item".
                3. Reply in JSON.
                
                OUTPUT FORMAT:
                {
                    "intent": "chat" OR "order",
                    "reasoning": "...",
                    "suggested_item": "Food Name",
                    "message": "..."
                }
                USER SAYS: 
                """.formatted(userProfileJson);

            // 3. Call Gemini
            String finalPrompt = systemPrompt + question;
            String jsonBody = """
                { "contents": [{ "parts": [{"text": "%s"}] }] }
                """.formatted(finalPrompt.replace("\n", "\\n").replace("\"", "\\\""));
                
            String googleUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            String rawJson = restClient.post().uri(googleUrl).header("Content-Type", "application/json").body(jsonBody).retrieve().body(String.class);

            // 4. Parse Gemini Response
            JsonNode root = objectMapper.readTree(rawJson);
            String aiText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            String cleanJson = aiText.replace("```json", "").replace("```", "").trim();
            
            // Convert JSON string -> Java Object (AgentResponse)
            AgentResponse response = objectMapper.readValue(cleanJson, AgentResponse.class);

            // --- 🤖 AGENTIC EXECUTION LAYER 🤖 ---
            // This is where the machine takes action!
            if ("order".equalsIgnoreCase(response.intent) && response.suggestedItem != null) {
                
                System.out.println("🚀 AGENT DECIDED TO ORDER: " + response.suggestedItem);
                
                // A. Call Swiggy API (The Body)
                String orderStatus = fakeSwiggyController.placeOrder(response.suggestedItem);
                
                // B. Update User Memory (The Learning)
                // We assume a price of 500 for now, or fetch from Swiggy menu in a real app
                userProfileService.updateUserStats(userId, 500.0, "Italian"); // Example update
                
                // C. Update the message to the user
                response.message = response.message + " [SYSTEM: " + orderStatus + "]";
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