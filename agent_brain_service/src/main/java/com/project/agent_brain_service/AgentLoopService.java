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
import java.util.concurrent.ConcurrentHashMap;
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
    private static final int MAX_RETRIES = 12;

    // Current free-tier model (Feb 2026)
    private static final String MODEL_NAME = "gemini-2.5-flash";

    // ── Multi-API-Key Rotation ──
    // Set GEMINI_API_KEY="key1,key2,key3" in env for round-robin rotation.
    // With N keys, cooldown drops proportionally.
    private String[] apiKeys;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    // ── Per-Key Rate Tracking ──
    // Tracks the last API call timestamp per key to enforce minimum gaps.
    // Free tier: 15 RPM = 1 call per 4 seconds per key.
    private static final long MIN_GAP_PER_KEY_MS = 4000L; // 4.0s per key (15 RPM = 1 call/4s)
    private final ConcurrentHashMap<String, Long> lastCallTimePerKey = new ConcurrentHashMap<>();

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

    /**
     * Enforces a minimum time gap between calls to the same API key.
     * If the last call with this key was too recent, sleeps until it's safe.
     */
    private void waitForRateLimit(String apiKey) throws InterruptedException {
        Long lastCall = lastCallTimePerKey.get(apiKey);
        if (lastCall != null) {
            long elapsed = System.currentTimeMillis() - lastCall;
            long waitNeeded = MIN_GAP_PER_KEY_MS - elapsed;
            if (waitNeeded > 0) {
                System.out.println("⏱️ Rate limiter: waiting " + (waitNeeded / 1000.0) + "s before next call (key ..." + apiKey.substring(Math.max(0, apiKey.length() - 4)) + ")");
                Thread.sleep(waitNeeded);
            }
        }
    }

    /**
     * Records that an API call was just made with this key.
     */
    private void recordApiCall(String apiKey) {
        lastCallTimePerKey.put(apiKey, System.currentTimeMillis());
    }

    /**
     * Exponential backoff for retry waits.
     * Starts at 3s, doubles each retry, capped at 30s.
     */
    private long getExponentialBackoff(int retryCount) {
        long baseMs = 3_000L; // 3 seconds
        long backoff = baseMs * (1L << (retryCount - 1)); // 3s, 6s, 12s, 24s
        return Math.min(backoff, 30_000L); // cap at 30s
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

                // Per-key rate limiter handles spacing — no extra cooldown needed

                Map<String, Object> requestBody = buildGeminiRequest(userId, systemPrompt);
                String jsonBody = mapper.writeValueAsString(requestBody);

                String rawResponse = null;
                int retries = 0;
                long startTime = System.currentTimeMillis();

                while (retries < MAX_RETRIES) {
                    String currentKey = getNextApiKey();
                    // Enforce per-key rate limit BEFORE making the call
                    waitForRateLimit(currentKey);
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

                        // Record successful call time for rate tracking
                        recordApiCall(currentKey);
                        break; // Success

                    } catch (GeminiApiException apiEx) {
                        int status = apiEx.statusCode;
                        String body = apiEx.responseBody;

                        if (status == 429 || status == 503) {
                            retries++;
                            // Rotate to next key on rate limit
                            System.out.println("🔄 Rotating to next API key after HTTP " + status);
                            // Use exponential backoff, but also respect API's retryDelay if provided
                            long waitTime = getExponentialBackoff(retries);
                            try {
                                if (body.contains("retryDelay")) {
                                    // Extract seconds from "retryDelay": "13s" or "57s"
                                    int idx = body.indexOf("retryDelay");
                                    String sub = body.substring(idx, Math.min(idx + 50, body.length()));
                                    String[] parts = sub.split("\"\\s*:\\s*\"");
                                    if (parts.length > 1) {
                                        String delayStr = parts[1].replaceAll("[^0-9.]", "");
                                        double delaySec = Double.parseDouble(delayStr);
                                        // Use the LARGER of API's retryDelay+buffer and our exponential backoff
                                        waitTime = Math.max(waitTime, (long) (delaySec * 1000) + 3000);
                                    }
                                }
                            } catch (Exception parseEx) {
                                // Stick with exponential backoff
                            }
                            System.out.println("⏳ Rate limited (HTTP " + status + "). Waiting " + (waitTime / 1000) + "s... [Retry " + retries + "/" + MAX_RETRIES + "]");
                            // Let the retry loop handle backoff — no early abort
                            int totalKeys = (apiKeys != null) ? apiKeys.length : 1;
                            boolean hasMoreKeysToTry = (totalKeys > 1) && (retries % totalKeys != 0);

                            if (hasMoreKeysToTry) {
                                System.out.println("🔄 Rate limited on current key. Switching API key immediately...");
                                trace.add(new AgentResponse.TraceStep("🔄 Switching API Key", "", "Rate limit hit. Instant swap to backup key...", 0));
                            } else {
                                trace.add(new AgentResponse.TraceStep("⏳ Rate Limited (HTTP " + status + ")", "", "Waiting " + (waitTime / 1000) + "s... Retry " + retries + "/" + MAX_RETRIES, 0));
                                Thread.sleep(waitTime);
                                recordApiCall(currentKey);
                            }
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
                        if (retries >= MAX_RETRIES) {
                            throw new RuntimeException("Network error after " + MAX_RETRIES + " retries: " + err);
                        }
                        Thread.sleep(getExponentialBackoff(retries));
                    }
                }

                if (rawResponse == null) throw new RuntimeException("Google API failed after " + MAX_RETRIES + " retries. Check server console for details.");
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

                JsonNode partsArray = root.path("candidates").get(0).path("content").path("parts");

                boolean madeToolCall = false;
                List<Map<String, Object>> functionResponses = new ArrayList<>();

                for (JsonNode part : partsArray) {
                    if (part.has("functionCall")) {
                        madeToolCall = true;
                        JsonNode funcCall = part.path("functionCall");
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
                        
                        functionResponses.add(Map.of("name", toolName, "response", Map.of("content", parsedResult)));
                    }
                }

                if (madeToolCall) {
                    // Send all function responses to history as a single message
                    Map<String, Object> message = new HashMap<>();
                    message.put("role", "user");
                    List<Map<String, Object>> partsList = new ArrayList<>();
                    for (Map<String, Object> fr : functionResponses) {
                        partsList.add(Map.of("functionResponse", fr));
                    }
                    message.put("parts", partsList);
                    historyService.getHistory(userId).add(message);
                    continue;
                }

                // If no function calls, process text
                for (JsonNode part : partsArray) {
                    if (part.has("text")) {
                        String aiText = part.path("text").asText();
                        historyService.addModelTextResponse(userId, aiText);
                        long totalElapsed = System.currentTimeMillis() - loopStartTime;
                        trace.add(new AgentResponse.TraceStep("💬 Response", "", truncate(aiText, 150), duration));
                        System.out.println("⚡ Agent loop completed in " + (totalElapsed / 1000.0) + "s (" + iterations + " iteration(s))");
                        response.message = aiText;
                        response.confidence = 100;
                        response.trace = trace;
                        return response;
                    }
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

                // Per-key rate limiter handles spacing — no extra cooldown needed

                Map<String, Object> requestBody = buildGeminiRequest(userId, systemPrompt);
                String jsonBody = mapper.writeValueAsString(requestBody);

                String rawResponse = null;
                int retries = 0;
                long startTime = System.currentTimeMillis();

                while (retries < MAX_RETRIES) {
                    String currentKey = getNextApiKey();
                    // Enforce per-key rate limit BEFORE making the call
                    waitForRateLimit(currentKey);
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
                        // Record successful call time for rate tracking
                        recordApiCall(currentKey);
                        break;

                    } catch (GeminiApiException apiEx) {
                        int status = apiEx.statusCode;
                        String body = apiEx.responseBody;

                        if (status == 429 || status == 503) {
                            retries++;
                            // Use exponential backoff, but also respect API's retryDelay if provided
                            long waitTime = getExponentialBackoff(retries);
                            try {
                                if (body.contains("retryDelay")) {
                                    int idx = body.indexOf("retryDelay");
                                    String sub = body.substring(idx, Math.min(idx + 50, body.length()));
                                    String[] parts = sub.split("\"\\s*:\\s*\"");
                                    if (parts.length > 1) {
                                        String delayStr = parts[1].replaceAll("[^0-9.]", "");
                                        double delaySec = Double.parseDouble(delayStr);
                                        waitTime = Math.max(waitTime, (long) (delaySec * 1000) + 3000);
                                    }
                                }
                            } catch (Exception parseEx) {
                                // Stick with exponential backoff
                            }
                            // Let the retry loop handle backoff — no early abort
                            int totalKeys = (apiKeys != null) ? apiKeys.length : 1;
                            boolean hasMoreKeysToTry = (totalKeys > 1) && (retries % totalKeys != 0);

                            if (hasMoreKeysToTry) {
                                System.out.println("🔄 [STREAM] Rate limited on current key. Switching API key immediately...");
                                AgentResponse.TraceStep swapStep = new AgentResponse.TraceStep("🔄 Switching API Key", "", "Rate limit hit. Instant swap to backup key...", 0);
                                trace.add(swapStep);
                                emitter.send(SseEmitter.event().name("trace").data(mapper.writeValueAsString(swapStep)));
                            } else {
                                AgentResponse.TraceStep retryStep = new AgentResponse.TraceStep("⏳ Rate Limited (HTTP " + status + ")", "", "Waiting " + (waitTime / 1000) + "s... Retry " + retries + "/" + MAX_RETRIES, 0);
                                trace.add(retryStep);
                                emitter.send(SseEmitter.event().name("trace").data(mapper.writeValueAsString(retryStep)));
                                Thread.sleep(waitTime);
                                recordApiCall(currentKey);
                            }
                        } else {
                            throw new RuntimeException("Gemini API error (HTTP " + status + "): " + truncate(body, 300));
                        }
                    } catch (RuntimeException ex) {
                        throw ex;
                    } catch (Exception e) {
                        retries++;
                        if (retries >= MAX_RETRIES) throw new RuntimeException("Network error after " + MAX_RETRIES + " retries: " + e.getMessage());
                        Thread.sleep(getExponentialBackoff(retries));
                    }
                }

                if (rawResponse == null) throw new RuntimeException("Google API failed after " + MAX_RETRIES + " retries.");
                long duration = System.currentTimeMillis() - startTime;

                JsonNode root = mapper.readTree(rawResponse);
                if (!root.has("candidates") || root.path("candidates").isEmpty()) {
                    response.message = "⚠️ AI returned no content.";
                    response.trace = trace;
                    emitter.send(SseEmitter.event().name("result").data(mapper.writeValueAsString(response)));
                    emitter.complete();
                    return;
                }

                JsonNode partsArray = root.path("candidates").get(0).path("content").path("parts");

                boolean madeToolCall = false;
                List<Map<String, Object>> functionResponses = new ArrayList<>();

                for (JsonNode part : partsArray) {
                    if (part.has("functionCall")) {
                        madeToolCall = true;
                        JsonNode funcCall = part.path("functionCall");
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
                        
                        functionResponses.add(Map.of("name", toolName, "response", Map.of("content", parsedResult)));
                    }
                }

                if (madeToolCall) {
                    Map<String, Object> message = new HashMap<>();
                    message.put("role", "user");
                    List<Map<String, Object>> partsList = new ArrayList<>();
                    for (Map<String, Object> fr : functionResponses) {
                        partsList.add(Map.of("functionResponse", fr));
                    }
                    message.put("parts", partsList);
                    historyService.getHistory(userId).add(message);
                    continue;
                }

                for (JsonNode part : partsArray) {
                    if (part.has("text")) {
                        String aiText = part.path("text").asText();
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
    private static final int MAX_HISTORY_MESSAGES = 10;

    /**
     * Safely trims conversation history to ensure it doesn't violate Gemini API constraints:
     * 1. The first message must have role="user"
     * 2. functionCall and functionResponse pairs must not be split
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> trimHistorySafely(List<Map<String, Object>> fullHistory, int maxMessages) {
        if (fullHistory.size() <= maxMessages) {
            return fullHistory;
        }

        int startIndex = fullHistory.size() - maxMessages;

        // Walk forward to find a safe start index.
        // A safe start is a "user" role message that is NOT a "functionResponse".
        while (startIndex < fullHistory.size()) {
            Map<String, Object> msg = fullHistory.get(startIndex);
            String role = (String) msg.get("role");

            if ("user".equals(role)) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) msg.get("parts");
                boolean isFunctionResponse = false;
                
                if (parts != null && !parts.isEmpty()) {
                    for (Map<String, Object> part : parts) {
                        if (part.containsKey("functionResponse")) {
                            isFunctionResponse = true;
                            break;
                        }
                    }
                }
                
                if (!isFunctionResponse) {
                    break; // Found a pure user text message to safely begin the trimmed context
                }
            }
            startIndex++;
        }

        // Fallback: if we couldn't find a safe start, just return the full history
        // to avoid API errors (though it uses more tokens).
        if (startIndex >= fullHistory.size()) {
            return fullHistory;
        }

        return new ArrayList<>(fullHistory.subList(startIndex, fullHistory.size()));
    }

    private Map<String, Object> buildGeminiRequest(String userId, String systemPrompt) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));

        // Trim conversation history to last N messages to reduce token count and speed up inference
        List<Map<String, Object>> fullHistory = historyService.getHistory(userId);
        List<Map<String, Object>> trimmed = trimHistorySafely(fullHistory, MAX_HISTORY_MESSAGES);
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

                AUTONOMY: Level=%s | Cuisine Confidence: {%s} | Restaurant Prefs: {%s} | Language: %s
                • FULL_AUTO: Act immediately, no confirmation needed.
                • BALANCED: confidence>=0.6 → proceed; <0.6 or unknown → ask first.
                • CONSERVATIVE: Always ask before ordering.

                🧠 MEMORY-BASED ORDERING (HIGH PRIORITY):
                - When the user requests a specific food type (e.g. "I want burger", "get me biryani"), you MUST:
                  1. FIRST call get_order_history.
                  2. Check if the history contains the EXACT food type they just requested (e.g. if they asked for a burger, look ONLY for past burger orders).
                  3. If you find a matching past order for that specific food: Say e.g. "You ordered a burger from Burger King last time—should I go ahead with them again?"
                  4. If they say NO, or if they have NEVER ordered that specific food before, then proceed to search_menu.
                  CRITICAL: NEVER suggest a past order that doesn't match what the user is currently asking for (e.g. if they want a burger, do NOT suggest their past pizza order!).

                CRITICAL RULES:
                - ALWAYS respond to the user strictly in their preferred language: %s (Translate your final natural language response).
                - On user confirmation ("yes"/"sure"/"go ahead"), complete the ENTIRE remaining workflow in ONE turn. Never pause mid-flow.
                - Once a restaurant is selected/named by the user, IMMEDIATELY complete the FULL order. NEVER say "shall I order?".

                CART WORKFLOW:
                1. search_menu (always pass userId)
                2. If multiple restaurants found → rank_restaurants, present top options
                3. Once items/restaurant selected:
                   a. FIRST call view_cart — if ANY old items exist from a previous order, call remove_from_cart for EACH old item to clear the cart. Only the NEW items the user just requested should be in the cart.
                   b. evaluate_coupons → pick best
                   c. check_wallet → verify funds
                   d. If wallet balance is INSUFFICIENT: Calculate the shortfall amount and ASK the user: "Your balance is ₹X but the order costs ₹Y. Shall I add ₹Z to your wallet?" If they say yes, call add_money with the shortfall amount, then continue checkout.
                   e. add_to_cart for EACH item requested, including any requested customizations
                   f. checkout_cart with best coupon
                   g. Report order confirmation
                   Do NOT pause between these steps.

                SMART FEATURES:
                - "I don't know what to eat" / "Recommend": Call get_recommendations.
                - "Order what I had last time" / "Reorder": Call get_order_history, then reorder.

                CANCELLATION (cancel_order): PLACED=full refund, PREPARING=50%% refund, OUT_FOR_DELIVERY/DELIVERED=cannot cancel.
                ISSUES (report_issue on delivered orders): WRONG_ITEM/MISSING_ITEM/NEVER_DELIVERED=full refund, COLD_FOOD/BAD_QUALITY=50%% refund.

                TONE: Friendly, concise, action-oriented. Emojis sparingly.
                """.formatted(
                    userId, profile.budget.rangeMin, profile.budget.rangeMax, profile.autonomyLevel,
                    allergies, dislikes, blacklistedRestaurants,
                    profile.autonomyLevel,
                    confidenceStr.toString(), restPrefStr.toString(), profile.languagePreference, profile.languagePreference
                );
    }

    private String getToolEmoji(String name) {
        if (name.contains("search")) return "🔍";
        if (name.contains("wallet")) return "💰";
        if (name.contains("cart")) return "🛒";
        if (name.contains("order") && !name.contains("cancel")) return "🛒";
        if (name.contains("reorder") || name.contains("history")) return "🔁";
        if (name.contains("recommendation")) return "💡";
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