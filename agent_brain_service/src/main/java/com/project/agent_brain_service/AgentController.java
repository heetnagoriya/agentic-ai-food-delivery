package com.project.agent_brain_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The main /ask endpoint + /ask/stream SSE endpoint for real-time progress.
 */
@RestController
public class AgentController {

    @Autowired
    private AgentLoopService agentLoopService;

    /**
     * Original synchronous endpoint (kept for backwards compatibility).
     */
    @GetMapping("/ask")
    public AgentResponse askAgent(
            @RequestParam(value = "userId", defaultValue = "user_123") String userId,
            @RequestParam(value = "question", defaultValue = "I am hungry") String question) {
        return agentLoopService.runAgentLoop(userId, question);
    }

    /**
     * SSE streaming endpoint — sends trace steps in real-time as the agent loop runs.
     * Events:
     *   "trace"  → JSON TraceStep as each tool call completes
     *   "result" → final JSON AgentResponse
     *   "error"  → error message if something fails
     */
    @GetMapping("/ask/stream")
    public SseEmitter askAgentStream(
            @RequestParam(value = "userId", defaultValue = "user_123") String userId,
            @RequestParam(value = "question", defaultValue = "I am hungry") String question) {

        // 3-minute timeout (agent loop can take a while with retries)
        SseEmitter emitter = new SseEmitter(180_000L);

        // Run in a separate thread so the SSE connection stays open
        new Thread(() -> agentLoopService.runAgentLoopStreaming(userId, question, emitter))
                .start();

        return emitter;
    }
}