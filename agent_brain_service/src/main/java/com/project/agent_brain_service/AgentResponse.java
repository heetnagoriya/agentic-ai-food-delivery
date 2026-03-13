package com.project.agent_brain_service;

import java.util.ArrayList;
import java.util.List;

/**
 * Response from the Agent to the frontend dashboard.
 * 
 * BEFORE: Had intent, suggestedItem, restaurantId, couponCode, finalPrice
 *         (because Java code had to interpret AI output and act on it).
 * 
 * AFTER:  The AI acts directly through tool calls (search, order, etc.).
 *         The response is now:
 *         - message: the AI's natural language reply
 *         - trace: step-by-step log of every tool the AI called (for the dashboard)
 *         - confidence: how many steps the AI took (more steps = more thorough)
 */
public class AgentResponse {

    public String message;
    public int confidence;
    public List<TraceStep> trace = new ArrayList<>();

    /**
     * One step in the AI's decision-making pipeline.
     * Displayed in the "Agent Brain" panel on the dashboard.
     * 
     * Example:
     *   step: "🔍 search_menu"
     *   input: {"query": "pizza"}
     *   output: "Found 3 results: Pizza (₹600), Burger (₹150)..."
     *   durationMs: 450
     */
    public static class TraceStep {
        public String step;
        public String input;
        public String output;
        public long durationMs;

        public TraceStep(String step, String input, String output, long durationMs) {
            this.step = step;
            this.input = input;
            this.output = output;
            this.durationMs = durationMs;
        }
    }
}