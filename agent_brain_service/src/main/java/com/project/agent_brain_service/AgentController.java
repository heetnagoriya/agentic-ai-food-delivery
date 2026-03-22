package com.project.agent_brain_service;

import com.project.agent_brain_service.service.DynamoDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The main /ask endpoint + /ask/stream SSE endpoint for real-time progress.
 * After each successful response, the turn is persisted to DynamoDB.
 */
@RestController
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AgentController {

    @Autowired
    private AgentLoopService agentLoopService;

    @Autowired
    private DynamoDbService dbService;

    /**
     * Synchronous ask — runs the agent loop and returns the complete response.
     * Saves the user message + AI response to DynamoDB chat history.
     */
    @GetMapping("/ask")
    public AgentResponse askAgent(
            @RequestParam(value = "userId", defaultValue = "user_123") String userId,
            @RequestParam(value = "question", defaultValue = "I am hungry") String question) {

        AgentResponse response = agentLoopService.runAgentLoop(userId, question);

        // Persist this conversation turn to DynamoDB
        try {
            ChatSession session = dbService.getChat(userId);
            if (session == null) session = new ChatSession(userId);

            long ts = System.currentTimeMillis();
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));

            session.messages.add(new ChatSession.ChatMessage(ts, "user", question, time, 0, null));
            session.messages.add(new ChatSession.ChatMessage(ts + 1, "ai", response.message, time, response.confidence, response.trace));

            // Keep only last 100 messages (50 turns) to avoid unbounded growth
            if (session.messages.size() > 100) {
                session.messages = session.messages.subList(session.messages.size() - 100, session.messages.size());
            }

            dbService.saveChat(session);
        } catch (Exception e) {
            System.err.println("WARN: Could not persist chat history: " + e.getMessage());
        }

        return response;
    }

    /**
     * SSE streaming endpoint — sends trace steps in real-time as the agent loop runs.
     */
    @GetMapping("/ask/stream")
    public SseEmitter askAgentStream(
            @RequestParam(value = "userId", defaultValue = "user_123") String userId,
            @RequestParam(value = "question", defaultValue = "I am hungry") String question) {

        SseEmitter emitter = new SseEmitter(180_000L);
        new Thread(() -> agentLoopService.runAgentLoopStreaming(userId, question, emitter))
                .start();
        return emitter;
    }
}