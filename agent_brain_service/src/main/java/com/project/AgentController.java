package com.project.agent_brain_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class AgentController {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/ask")
    public String askAgent(@RequestParam(value = "question", defaultValue = "Hello") String question) {
        
        // We stick with the MODEL THAT WORKS: gemini-2.5-flash
        String googleUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String jsonBody = """
            {
                "contents": [{
                    "parts": [{"text": "%s"}]
                }]
            }
            """.formatted(question);

        try {
            // 1. Get Raw JSON
            String rawJson = restClient.post()
                    .uri(googleUrl)
                    .header("Content-Type", "application/json")
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            // 2. Extract JUST the answer text
            JsonNode root = objectMapper.readTree(rawJson);
            String answer = root.path("candidates")
                                .get(0)
                                .path("content")
                                .path("parts")
                                .get(0)
                                .path("text")
                                .asText();
            
            return answer;

        } catch (Exception e) {
            return "🤖 AI Error: " + e.getMessage();
        }
    }
}