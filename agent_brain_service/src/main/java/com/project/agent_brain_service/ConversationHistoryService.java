package com.project.agent_brain_service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores FULL conversation history per user in the exact Gemini API format.
 * 
 * BEFORE: ConversationStateService only remembered 1 suggestion (item + price).
 *         "Yes" broke because there was no conversation context.
 * 
 * AFTER:  Every user message, AI response, tool call, and tool result is stored.
 *         The full history is sent to Gemini so it naturally understands context.
 *         When user says "Yes", the AI sees what it previously suggested.
 */
@Service
public class ConversationHistoryService {

    // userId -> ordered list of Gemini API content objects
    private final Map<String, List<Map<String, Object>>> histories = new ConcurrentHashMap<>();

    /**
     * Add a user's text message to the conversation.
     * Gemini format: { "role": "user", "parts": [{ "text": "..." }] }
     */
    public void addUserMessage(String userId, String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", text);
        parts.add(part);
        message.put("parts", parts);
        getHistory(userId).add(message);
    }

    /**
     * Add the AI's final text response to the conversation.
     * Gemini format: { "role": "model", "parts": [{ "text": "..." }] }
     */
    public void addModelTextResponse(String userId, String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "model");
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", text);
        parts.add(part);
        message.put("parts", parts);
        getHistory(userId).add(message);
    }

    /**
     * Record that the AI decided to call a tool.
     * Gemini format: { "role": "model", "parts": [{ "functionCall": { "name": "...", "args": {...} } }] }
     */
    public void addFunctionCall(String userId, String name, Map<String, Object> args) {
        Map<String, Object> funcCall = new HashMap<>();
        funcCall.put("name", name);
        funcCall.put("args", args != null ? args : new HashMap<>());

        Map<String, Object> part = new HashMap<>();
        part.put("functionCall", funcCall);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "model");
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(part);
        message.put("parts", parts);

        getHistory(userId).add(message);
    }

    /**
     * Send the tool's result back to the AI.
     * Gemini format: { "role": "user", "parts": [{ "functionResponse": { "name": "...", "response": { "content": {...} } } }] }
     */
    public void addFunctionResponse(String userId, String name, Object result) {
        Map<String, Object> responseContent = new HashMap<>();
        responseContent.put("content", result);

        Map<String, Object> funcResponse = new HashMap<>();
        funcResponse.put("name", name);
        funcResponse.put("response", responseContent);

        Map<String, Object> part = new HashMap<>();
        part.put("functionResponse", funcResponse);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(part);
        message.put("parts", parts);

        getHistory(userId).add(message);
    }

    /**
     * Get the full conversation history for a user.
     * Creates a new empty list if user has no history.
     */
    public List<Map<String, Object>> getHistory(String userId) {
        return histories.computeIfAbsent(userId, k -> new ArrayList<>());
    }

    /**
     * Wipe all memory for a user (used by the "Reset" button).
     */
    public void clearHistory(String userId) {
        histories.remove(userId);
    }
}
