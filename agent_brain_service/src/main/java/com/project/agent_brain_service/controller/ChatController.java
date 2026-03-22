package com.project.agent_brain_service.controller;

import com.project.agent_brain_service.ChatSession;
import com.project.agent_brain_service.service.DynamoDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exposes persistent chat history stored in DynamoDB.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ChatController {

    @Autowired
    private DynamoDbService dbService;

    /**
     * GET /api/chat/history?userId=xxx
     * Returns the full list of persisted chat messages for the given user.
     */
    @GetMapping("/history")
    public List<ChatSession.ChatMessage> getChatHistory(
            @RequestParam String userId) {
        try {
            ChatSession session = dbService.getChat(userId);
            if (session == null) return Collections.emptyList();
            return session.messages;
        } catch (Exception e) {
            System.err.println("WARN: Could not load chat history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * DELETE /api/chat/history?userId=xxx
     * Clears the user's persisted chat history (used by "New Chat").
     */
    @DeleteMapping("/history")
    public Map<String, String> clearChatHistory(@RequestParam String userId) {
        try {
            ChatSession blank = new ChatSession(userId);
            dbService.saveChat(blank);
            return Map.of("status", "ok", "message", "Chat history cleared.");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
