package com.project.agent_brain_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AgentLoopService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKeyConfig;

    @Autowired private AgentTool agentTool;
    @Autowired private ConversationHistoryService historyService;
    @Autowired private UserProfileService userProfileService;

    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final int MAX_ITERATIONS = 10;

    // Current free-tier model (Feb 2026)
    private static final String MODEL_NAME = "gemini-2.5-flash";

    // ── Multi-API-Key Rotation ──
    // Set GEMINI_API_KEY="key1,key2,key3" in env for round-robin rotation.
    // With N keys, cooldown drops from 14s to 14/N seconds per call.
    private String[] apiKeys;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    private String getNextApiKey() {
        if (apiKeys == null || apiKeys.length == 0) {
            // Lazy-init: parse comma-separated keys on first use
            apiKeys = Arrays.stream(apiKeyConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            if (apiKeys.length == 0) {
                throw new RuntimeException("No API keys configured! Set GEMINI_API_KEY env variable.");
            }
            System.out.println("🔑 Loaded " + apiKeys.length + " API key(s) for rotation.");
        }
        int idx = keyIndex.getAndUpdate(i -> (i + 1) % apiKeys.length);
        return apiKeys[idx];
    }

    private long getCooldownMs() {
        int numKeys = (apiKeys != null) ? apiKeys.length : 1;
        // Free tier: 15 RPM per key = 4s/req. With N keys: 4s/N.
        // Minimum 1s to avoid hammering.
        return Math.max(1000, 4000L / numKeys);
    }

    // v1beta required for system_instruction and tools (function calling)
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    public AgentResponse runAgentLoop(String userId, String question) {
        long loopStartTime = System.currentTimeMillis();
        AgentResponse response = new AgentResponse();
        List<AgentResponse.TraceStep> trace = new ArrayList<>();

        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            String systemPrompt = buildSystemPrompt(userId, profile);
            historyService.addUserMessage(userId, question);

            int iterations = 0;
            while (iterations < MAX_ITERATIONS) {
                iterations++;

                // Cooldown between calls — scales with number of API keys
                if (iterations > 1) {
                    long cooldown = getCooldownMs();
                    System.out.println("💤 Cooling down for " + (cooldown / 1000) + "s (" + (apiKeys != null ? apiKeys.length : 1) + " key(s))...");
                    Thread.sleep(cooldown);
                }

                Map<String, Object> requestBody = buildGeminiRequest(userId, systemPrompt);
                String jsonBody = mapper.writeValueAsString(requestBody);

                String rawResponse = null;
                int retries = 0;
                long startTime = System.currentTimeMillis();

                while (retries < 5) {
                    String currentKey = getNextApiKey();
                    String url = API_BASE + MODEL_NAME + ":generateContent?key=" + currentKey;

                    // Use atomic references to capture error info from status handlers
                    AtomicInteger httpStatus = new AtomicInteger(0);
                    AtomicReference<String> errorBody = new AtomicReference<>("");

                    try {
                        rawResponse = restClient.post()
                                .uri(url)
                                .header("Content-Type", "application/json")
                                .body(jsonBody)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, (req, res) -> {
                                    httpStatus.set(res.getStatusCode().value());
                                    byte[] body = res.getBody().readAllBytes();
                                    String bodyStr = new String(body);
                                    errorBody.set(bodyStr);
                                    System.out.println("🔴 Gemini API HTTP " + httpStatus.get() + ": " + bodyStr);
                                })
                                .body(String.class);

                        // If onStatus captured an error, rawResponse will be null
                        if (httpStatus.get() > 0) {
                            rawResponse = null;
                            throw new GeminiApiException(httpStatus.get(), errorBody.get());
                        }

                        break; // Success

                    } catch (GeminiApiException apiEx) {
                        int status = apiEx.statusCode;
                        String body = apiEx.responseBody;

                        if (status == 429 || status == 503) {
                            retries++;
                            // Rotate to next key on rate limit
                            System.out.println("🔄 Rotating to next API key after HTTP " + status);
                            // Parse retryDelay from API response if available
                            long waitTime = 8000L; // Default 8 seconds (reduced from 20s)
                            try {
                                if (body.contains("retryDelay")) {
                                    // Extract seconds from "retryDelay": "13s" or "57s"
                                    int idx = body.indexOf("retryDelay");
                                    String sub = body.substring(idx, Math.min(idx + 50, body.length()));
                                    String[] parts = sub.split("\"\\s*:\\s*\"");
                                    if (parts.length > 1) {
                                        String delayStr = parts[1].replaceAll("[^0-9.]", "");
                                        double delaySec = Double.parseDouble(delayStr);
                                        waitTime = (long) (delaySec * 1000) + 3000; // Add 3s buffer
                                    }
                                }
                            } catch (Exception parseEx) {
                                waitTime = 5000L * retries; // Fallback: exponential backoff
                            }
                            // Trust the API's retryDelay; just add a small safety buffer
                            waitTime = Math.max(waitTime, 3000L);
                            System.out.println("⏳ Rate limited (HTTP " + status + "). Waiting " + (waitTime / 1000) + "s... [Retry " + retries + "/5]");
                            trace.add(new AgentResponse.TraceStep("⏳ Rate Limited (HTTP " + status + ")", "", "Waiting " + (waitTime / 1000) + "s... Retry " + retries + "/5", 0));
                            Thread.sleep(waitTime);
                        } else if (status == 404) {
                            String msg = "Model '" + MODEL_NAME + "' not found. Check https://ai.google.dev/gemini-api/docs/models for current models.";
                            System.out.println("🔴 " + msg);
                            throw new RuntimeException(msg + " | API response: " + truncate(body, 300));
                        } else if (status == 400) {
                            System.out.println("🔴 Bad Request: " + body);
                            throw new RuntimeException("Gemini Bad Request (400): " + truncate(body, 300));
                        } else if (status == 403) {
                            System.out.println("🔴 Forbidden — API key may be invalid or restricted: " + body);
                            throw new RuntimeException("API key error (403): " + truncate(body, 300));
                        } else {
                            throw new RuntimeException("Gemini API error (HTTP " + status + "): " + truncate(body, 300));
                        }
                    } catch (RuntimeException ex) {
                        throw ex; // re-throw RuntimeExceptions
                    } catch (Exception e) {
                        // Unexpected errors (network timeout, DNS, etc.)
                        String err = e.getMessage();
                        System.out.println("⚠️ Unexpected error calling Gemini: " + err);
                        e.printStackTrace();
                        retries++;
                        if (retries >= 5) {
                            throw new RuntimeException("Network error after 5 retries: " + err);
                        }
                        Thread.sleep(5000L * retries);
                    }
                }

                if (rawResponse == null) throw new RuntimeException("Google API failed after 5 retries. Check server console for details.");
                long duration = System.currentTimeMillis() - startTime;

                JsonNode root = mapper.readTree(rawResponse);
                if (!root.has("candidates") || root.path("candidates").isEmpty()) {
                    // Check for prompt feedback / safety blocks
                    if (root.has("promptFeedback")) {
                        String feedback = root.path("promptFeedback").toString();
                        System.out.println("⚠️ Prompt blocked: " + feedback);
                        response.message = "⚠️ AI blocked the request. Prompt feedback: " + truncate(feedback, 200);
                    } else {
                        response.message = "⚠️ AI returned no content. Raw: " + truncate(rawResponse, 200);
                    }
                    response.trace = trace;
                    return response;
                }

                JsonNode firstPart = root.path("candidates").get(0).path("content").path("parts").get(0);

                if (firstPart.has("functionCall")) {
                    JsonNode funcCall = firstPart.path("functionCall");
                    String toolName = funcCall.path("name").asText();
                    Map<String, Object> toolArgs = mapper.convertValue(funcCall.path("args"), Map.class);

                    long toolStart = System.currentTimeMillis();
                    String toolResult = agentTool.executeTool(toolName, toolArgs);

                    trace.add(new AgentResponse.TraceStep(
                            getToolEmoji(toolName) + " " + toolName,
                            mapper.writeValueAsString(toolArgs),
                            truncate(toolResult, 250),
                            duration + (System.currentTimeMillis() - toolStart)
                    ));

                    historyService.addFunctionCall(userId, toolName, toolArgs);

                    Object parsedResult;
                    try { parsedResult = mapper.readValue(toolResult, Map.class); }
                    catch (Exception e) { parsedResult = Map.of("text_result", toolResult); }
                    historyService.addFunctionResponse(userId, toolName, parsedResult);

                    // (cooldown handled at top of loop — 14s between iterations)

                    continue;
                }

                if (firstPart.has("text")) {
                    String aiText = firstPart.path("text").asText();
                    historyService.addModelTextResponse(userId, aiText);
                    long totalElapsed = System.currentTimeMillis() - loopStartTime;
                    trace.add(new AgentResponse.TraceStep("💬 Response", "", truncate(aiText, 150), duration));
                    System.out.println("⚡ Agent loop completed in " + (totalElapsed / 1000.0) + "s (" + iterations + " iteration(s))");
                    response.message = aiText;
                    response.confidence = 100;
                    response.trace = trace;
                    return response;
                }
                break;
            }

            // Fallback: if the loop exhausted all iterations on tool calls
            // without producing a final text, summarize what happened
            if (response.message == null || response.message.isEmpty()) {
                StringBuilder summary = new StringBuilder("✅ I completed the following actions:\n");
                for (AgentResponse.TraceStep step : trace) {
                    if (step.output != null && !step.output.isEmpty()) {
                        summary.append("• ").append(step.step).append(": ").append(step.output).append("\n");
                    }
                }
                response.message = summary.toString();
                response.confidence = 80;
                response.trace = trace;
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.message = "🚨 Error: " + e.getMessage();
            response.trace = trace;
        }

        return response;
    }

    // ==================== SSE STREAMING VERSION ====================

    /**
     * Streaming version of runAgentLoop. Sends SSE events for each trace step
     * so the frontend can show real-time progress.
     * Events: "trace" (per tool call), "result" (final response), "error" (on failure)
     */
    public void runAgentLoopStreaming(String userId, String question, SseEmitter emitter) {
        long loopStartTime = System.currentTimeMillis();
        AgentResponse response = new AgentResponse();
        List<AgentResponse.TraceStep> trace = new ArrayList<>();

        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            String systemPrompt = buildSystemPrompt(userId, profile);
            historyService.addUserMessage(userId, question);

            int iterations = 0;
            while (iterations < MAX_ITERATIONS) {
                iterations++;

                if (iterations > 1) {
                    long cooldown = getCooldownMs();
                    System.out.println("💤 [STREAM] Cooling down for " + (cooldown / 1000) + "s...");
                    Thread.sleep(cooldown);
                }

                Map<String, Object> requestBody = buildGeminiRequest(userId, systemPrompt);
                String jsonBody = mapper.writeValueAsString(requestBody);

                String rawResponse = null;
                int retries = 0;
                long startTime = System.currentTimeMillis();

                while (retries < 5) {
                    String currentKey = getNextApiKey();
                    String url = API_BASE + MODEL_NAME + ":generateContent?key=" + currentKey;

                    AtomicInteger httpStatus = new AtomicInteger(0);
                    AtomicReference<String> errorBody = new AtomicReference<>("");

                    try {
                        rawResponse = restClient.post()
                                .uri(url)
                                .header("Content-Type", "application/json")
                                .body(jsonBody)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, (req, res) -> {
                                    httpStatus.set(res.getStatusCode().value());
                                    byte[] body = res.getBody().readAllBytes();
                                    errorBody.set(new String(body));
                                })
                                .body(String.class);

                        if (httpStatus.get() > 0) {
                            rawResponse = null;
                            throw new GeminiApiException(httpStatus.get(), errorBody.get());
                        }
                        break;

                    } catch (GeminiApiException apiEx) {
                        int status = apiEx.statusCode;
                        String body = apiEx.responseBody;

                        if (status == 429 || status == 503) {
                            retries++;
                            long waitTime = 8000L;
                            try {
                                if (body.contains("retryDelay")) {
                                    int idx = body.indexOf("retryDelay");
                                    String sub = body.substring(idx, Math.min(idx + 50, body.length()));
                                    String[] parts = sub.split("\"\\s*:\\s*\"");
                                    if (parts.length > 1) {
                                        String delayStr = parts[1].replaceAll("[^0-9.]", "");
                                        double delaySec = Double.parseDouble(delayStr);
                                        waitTime = (long) (delaySec * 1000) + 3000;
                                    }
                                }
                            } catch (Exception parseEx) {
                                waitTime = 5000L * retries;
                            }
                            waitTime = Math.max(waitTime, 3000L);
                            AgentResponse.TraceStep retryStep = new AgentResponse.TraceStep(
                                    "⏳ Rate Limited (HTTP " + status + ")", "",
                                    "Waiting " + (waitTime / 1000) + "s... Retry " + retries + "/5", 0);
                            trace.add(retryStep);
                            emitter.send(SseEmitter.event().name("trace").data(mapper.writeValueAsString(retryStep)));
                            Thread.sleep(waitTime);
                        } else {
                            throw new RuntimeException("Gemini API error (HTTP " + status + "): " + truncate(body, 300));
                        }
                    } catch (RuntimeException ex) {
                        throw ex;
                    } catch (Exception e) {
                        retries++;
                        if (retries >= 5) throw new RuntimeException("Network error after 5 retries: " + e.getMessage());
                        Thread.sleep(5000L * retries);
                    }
                }

                if (rawResponse == null) throw new RuntimeException("Google API failed after 5 retries.");
                long duration = System.currentTimeMillis() - startTime;

                JsonNode root = mapper.readTree(rawResponse);
                if (!root.has("candidates") || root.path("candidates").isEmpty()) {
                    response.message = "⚠️ AI returned no content.";
                    response.trace = trace;
                    emitter.send(SseEmitter.event().name("result").data(mapper.writeValueAsString(response)));
                    emitter.complete();
                    return;
                }

                JsonNode firstPart = root.path("candidates").get(0).path("content").path("parts").get(0);

                if (firstPart.has("functionCall")) {
                    JsonNode funcCall = firstPart.path("functionCall");
                    String toolName = funcCall.path("name").asText();
                    Map<String, Object> toolArgs = mapper.convertValue(funcCall.path("args"), Map.class);

                    long toolStart = System.currentTimeMillis();
                    String toolResult = agentTool.executeTool(toolName, toolArgs);

                    AgentResponse.TraceStep traceStep = new AgentResponse.TraceStep(
                            getToolEmoji(toolName) + " " + toolName,
                            mapper.writeValueAsString(toolArgs),
                            truncate(toolResult, 250),
                            duration + (System.currentTimeMillis() - toolStart)
                    );
                    trace.add(traceStep);

                    // 🔥 Stream this trace step to the frontend immediately
                    emitter.send(SseEmitter.event().name("trace").data(mapper.writeValueAsString(traceStep)));

                    historyService.addFunctionCall(userId, toolName, toolArgs);

                    Object parsedResult;
                    try { parsedResult = mapper.readValue(toolResult, Map.class); }
                    catch (Exception e) { parsedResult = Map.of("text_result", toolResult); }
                    historyService.addFunctionResponse(userId, toolName, parsedResult);

                    continue;
                }

                if (firstPart.has("text")) {
                    String aiText = firstPart.path("text").asText();
                    historyService.addModelTextResponse(userId, aiText);
                    long totalElapsed = System.currentTimeMillis() - loopStartTime;
                    trace.add(new AgentResponse.TraceStep("💬 Response", "", truncate(aiText, 150), duration));
                    System.out.println("⚡ [STREAM] Agent loop completed in " + (totalElapsed / 1000.0) + "s (" + iterations + " iteration(s))");
                    response.message = aiText;
                    response.confidence = 100;
                    response.trace = trace;

                    emitter.send(SseEmitter.event().name("result").data(mapper.writeValueAsString(response)));
                    emitter.complete();
                    return;
                }
                break;
            }

            // Fallback
            if (response.message == null || response.message.isEmpty()) {
                StringBuilder summary = new StringBuilder("✅ I completed the following actions:\n");
                for (AgentResponse.TraceStep step : trace) {
                    if (step.output != null && !step.output.isEmpty()) {
                        summary.append("• ").append(step.step).append(": ").append(step.output).append("\n");
                    }
                }
                response.message = summary.toString();
                response.confidence = 80;
                response.trace = trace;
            }

            emitter.send(SseEmitter.event().name("result").data(mapper.writeValueAsString(response)));
            emitter.complete();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                emitter.send(SseEmitter.event().name("error").data("🚨 Error: " + e.getMessage()));
                emitter.complete();
            } catch (Exception emitErr) {
                emitter.completeWithError(emitErr);
            }
        }
    }

    // ==================== HELPERS ====================
    private static final int MAX_HISTORY_MESSAGES = 20;

    private Map<String, Object> buildGeminiRequest(String userId, String systemPrompt) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));

        // Trim conversation history to last N messages to reduce token count and speed up inference
        List<Map<String, Object>> fullHistory = historyService.getHistory(userId);
        List<Map<String, Object>> trimmed = fullHistory.size() > MAX_HISTORY_MESSAGES
                ? new ArrayList<>(fullHistory.subList(fullHistory.size() - MAX_HISTORY_MESSAGES, fullHistory.size()))
                : fullHistory;
        request.put("contents", trimmed);

        request.put("tools", List.of(Map.of("functionDeclarations", agentTool.getFunctionDeclarations())));
        return request;
    }

    private String buildSystemPrompt(String userId, UserProfile profile) {
        // Build allergy/constraint strings
        String allergies = profile.preferences.allergies.isEmpty()
                ? "None" : String.join(", ", profile.preferences.allergies);
        String dislikes = profile.preferences.dislikes.isEmpty()
                ? "None" : String.join(", ", profile.preferences.dislikes);
        String blacklistedRestaurants = profile.preferences.blacklistedRestaurants.isEmpty()
                ? "None" : String.join(", ", profile.preferences.blacklistedRestaurants);

        // Build cuisine confidence string
        StringBuilder confidenceStr = new StringBuilder();
        if (profile.preferences.cuisineConfidence.isEmpty()) {
            confidenceStr.append("No cuisine history (all unknown)");
        } else {
            profile.preferences.cuisineConfidence.forEach((cuisine, conf) ->
                    confidenceStr.append(cuisine).append(": ").append(String.format("%.2f", conf)).append(", "));
        }

        // Build restaurant preference string
        StringBuilder restPrefStr = new StringBuilder();
        if (profile.restaurantPreferences.isEmpty()) {
            restPrefStr.append("No restaurant history");
        } else {
            profile.restaurantPreferences.forEach((restId, pref) ->
                    restPrefStr.append(restId).append(": ").append(String.format("%.2f", pref)).append(", "));
        }

        return """
                You are 'Foodie-Bot', a CONTROLLED AUTONOMOUS food delivery AI agent.
                User: %s | Budget: ₹%.0f-₹%.0f | Autonomy: %s

                HARD CONSTRAINTS (NEVER violate):
                - ALLERGIES: [%s]. NEVER suggest items containing these.
                - DISLIKES: [%s]. Avoid unless no alternative.
                - BLACKLISTED RESTAURANTS: [%s]. NEVER order from these.

                AUTONOMY: Level=%s | Cuisine Confidence: {%s} | Restaurant Prefs: {%s}
                • FULL_AUTO: Act immediately, no confirmation needed.
                • BALANCED: confidence>=0.6 → proceed; <0.6 or unknown → ask first.
                • CONSERVATIVE: Always ask before ordering.

                CRITICAL: On user confirmation ("yes"/"sure"/"go ahead"), complete the ENTIRE remaining workflow in ONE turn. Never pause mid-flow.

                WORKFLOW:
                1. search_menu (always pass userId)
                2. rank_restaurants if multiple results → present top 2-3, recommend #1
                3. Confirm if needed per autonomy rules
                4. evaluate_coupons → check_wallet → place_order → report ALL AT ONCE

                MULTI-RESTAURANT: Same item at multiple places → rank_restaurants, present top options with price/rating/preference/coupons. Honor specific restaurant requests directly.

                ORDER STATUS: track_order with order ID.
                COUPONS: Pick max rupee discount. Explain briefly.

                CANCELLATION (cancel_order): PLACED=full refund, PREPARING=50%% refund, OUT_FOR_DELIVERY/DELIVERED=cannot cancel.
                ISSUES (report_issue on delivered orders): WRONG_ITEM/MISSING_ITEM/NEVER_DELIVERED=full refund, COLD_FOOD/BAD_QUALITY=50%% refund.

                TONE: Friendly, concise, action-oriented. Emojis sparingly.
                """.formatted(
                    userId, profile.budget.rangeMin, profile.budget.rangeMax, profile.autonomyLevel,
                    allergies, dislikes, blacklistedRestaurants,
                    profile.autonomyLevel,
                    confidenceStr.toString(), restPrefStr.toString()
                );
    }

    private String getToolEmoji(String name) {
        if (name.contains("search")) return "🔍";
        if (name.contains("wallet")) return "💰";
        if (name.contains("order") && !name.contains("cancel")) return "🛒";
        if (name.contains("coupon")) return "🎟️";
        if (name.contains("rank")) return "📊";
        if (name.contains("track")) return "🛵";
        if (name.contains("cancel")) return "🚫";
        if (name.contains("report") || name.contains("issue")) return "⚠️";
        return "🔧";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * Custom exception to carry HTTP status code + response body from Gemini API errors.
     */
    private static class GeminiApiException extends Exception {
        final int statusCode;
        final String responseBody;

        GeminiApiException(int statusCode, String responseBody) {
            super("Gemini API HTTP " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }
}