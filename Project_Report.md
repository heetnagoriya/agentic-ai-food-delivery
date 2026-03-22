# G H Patel College of Engineering & Technology
**(A Constituent College of CVM University) V. V. Nagar**

## INFORMATION TECHNOLOGY DEPARTMENT

**Mini Project Report on**
# C.A.F.E.: Agentic AI Food Delivery System

**Submitted By:**
Name of Student: Heet Nagoriya  
Enrollment Number: [Insert Enrollment Here]

**Guided By:**
Prof. Nikhil Gondaliya

**MINI PROJECT (202040601)**  
**A.Y. 2025-26 EVEN TERM**

---

# CERTIFICATE

This is to certify that the Mini Project Report submitted entitled **“C.A.F.E.: Agentic AI Food Delivery System”** has been carried out by **Heet Nagoriya** under guidance in partial fulfillment for the Degree of Bachelor of Engineering in Information Technology, 6th Semester of G H Patel College of Engineering & Technology, CVM University, Vallabh Vidyanagar during the academic year 2025-26.

<br><br><br>

**Prof. Nikhil Gondaliya**  
(Internal Guide)
<br><br>
**Head of Department**  

---

# ACKNOWLEDGEMENT

I would like to express my deepest appreciation to all those who provided me the possibility to complete this project. A special gratitude I give to my internal guide, **Prof. Nikhil Gondaliya**, whose contribution in stimulating suggestions and encouragement helped me to coordinate my project effectively. 

Furthermore, I would also like to acknowledge with much appreciation the crucial role of the staff of the Information Technology Department, who gave the permission to use all required equipment and the necessary materials to complete the tasks. A special thanks goes to my classmates and colleagues who helped me assemble the parts and gave suggestions about the project.

Last but not least, I wish to avail myself of this opportunity to express a sense of gratitude and love to my friends and my beloved parents for their manual support, strength, help, and contributing towards the execution of this project.

**Heet Nagoriya**

---

# ABSTRACT

The rapid evolution of Artificial Intelligence and Large Language Models (LLMs) has opened new paradigms for human-computer interaction. Traditional food delivery platforms rely on static Graphical User Interfaces (GUIs) where users must manually search, apply filters, manage a cart, enter coupons, and navigate checkout flows. This often results in a cumbersome user experience, especially when users are indecisive or have complex dietary constraints.

This project, titled **C.A.F.E. (Controlled Autonomous Food Engine) / Agentic AI Food Delivery System**, introduces a novel, chat-centric approach to online food ordering. The system employs an autonomous AI Agent powered by Google's Gemini 2.5 Flash model, orchestrated via Spring AI and Spring Boot on the backend, with a highly responsive React 19 frontend. 

Instead of browsing menus, users simply send natural language requests (e.g., "I want a spicy chicken pizza without onions, use my best coupon"). The Agent Brain interprets the intent, respects the user's pre-configured dietary constraints (allergies, dislikes, blacklisted restaurants), and autonomously executes a sequence of tool calls (searching the menu, checking the wallet, applying coupons, adding to cart, and placing the order). 

The system implements a controlled autonomy architecture, allowing users to set the AI's independence level (Full Auto, Balanced, or Conservative) based on a calculated confidence score matrix. This ensures safety and user trust while minimizing the cognitive load required to order food. This report extensively details the problem domain, theoretical background, system architecture, module specifications, and the technical implementation of the C.A.F.E. system.

---

# Table of Contents
1. [INTRODUCTION](#1-introduction)
   1.1 [PROBLEM STATEMENT](#11-problem-statement)
   1.2 [PROJECT OVERVIEW](#12-project-overview)
   1.3 [AIM & OBJECTIVE OF THE PROJECT](#13-aim--objective-of-the-project)
2. [SYSTEM ANALYSIS](#2-system-analysis)
   2.1 [MOTIVATION](#21-motivation)
   2.2 [LITERATURE STUDY](#22-literature-study)
3. [DESIGN: ANALYSIS & METHODOLOGY](#3-design-analysis--methodology)
   3.1 [REQUIREMENT ANALYSIS](#31-requirement-analysis)
      3.1.1 [System Requirements](#311-system-requirements)
   3.2 [SYSTEM ARCHITECTURE](#32-system-architecture)
   3.3 [MODULE SPECIFICATION](#33-module-specification)
   3.4 [TIMELINE CHART](#34-timeline-chart)
   3.5 [UML DIAGRAMS](#35-uml-diagrams)
4. [IMPLEMENTATION](#4-implementation)
   4.1 [SYSTEM FLOW](#41-system-flow)
   4.2 [DATA FLOW DIAGRAMS](#42-data-flow-diagrams)
   4.3 [RESULTS](#43-results)
      4.3.1 [System Testing](#431-system-testing)
      4.3.2 [Relevant Screenshots](#432-relevant-screenshots)
5. [CONCLUSION AND FUTURE WORK](#conclusion-and-future-work)
6. [REFERENCES](#references)
7. [APPENDIX: SOURCE CODE](#appendix-source-code)

---

# List of Figures
- Figure 1: System Architecture Diagram
- Figure 2: Agent Tool Dispatcher Flow
- Figure 3: User Interface Design
- Figure 4: Order Execution Pipeline
- Figure 5: SSE (Server-Sent Events) Communication Flow

# List of Tables
- Table 1: Timeline Chart
- Table 2: API Tools Specification

---

# 1 INTRODUCTION

## 1.1 PROBLEM STATEMENT

In the contemporary digital landscape, food delivery applications (e.g., Swiggy, Zomato, UberEats) have become ubiquitous. However, the user experience on these platforms remains largely transactional and manual. A typical user journey involves opening the app, manually searching for a restaurant or cuisine, scrolling through extensive menus, reading reviews, manually applying dietary filters if available, comparing prices, managing a cart, searching for applicable coupon codes, verifying wallet balances, and finally checking out. 

This traditional approach presents several core problems:
1. **High Cognitive Load**: Users suffering from "decision fatigue" spend significant time deciding what to eat, often abandoning the process.
2. **Complex Dietary Constraints**: Users with severe allergies (e.g., peanuts, gluten) or specific dislikes must manually verify the ingredients of every single item, as current platforms lack robust, unified personalization architectures.
3. **Inefficient Deal Hunting**: Users must manually test multiple coupon codes to find the maximum discount, a process that is both tedious and error-prone.
4. **Lack of Conversational Memory**: Static apps do not natively remember that a user always orders "extra spicy" or prefers "Oven Story Pizza" over "Dominos" when requesting "pizza". 
5. **Rigid Workflows**: The flow of ordering cannot be organically adapted to the user's natural language input. 

There is a critical need for an intelligent system that abstracts away the GUI-driven complexity, allowing users to express their intent naturally while delegating the repetitive, mundane tasks (searching, applying coupons, checking out) to an autonomous software agent.

## 1.2 PROJECT OVERVIEW

The C.A.F.E. (Controlled Autonomous Food Engine) project is an advanced, end-to-end "Agentic AI Food Delivery System." Unlike standard chatbot integrations that merely answer questions, this system features a fully autonomous Agent Brain capable of executing real-world actions on behalf of the user.

The system is composed of two primary layers:
1. **The Chat-Centric Frontend**: Built entirely on React 19, this intuitive interface resembles a modern messaging application rather than a traditional food delivery app. It provides a real-time conversational canvas where the user talks to the AI, alongside dynamic state panels that visually render the AI's internal thought processes (Agent Trace), current cart payload, coupon evaluations, and live delivery maps.
2. **The Agent Brain Service (Backend)**: Developed using Spring Boot 3.4 and the Spring AI framework. This microservice connects to the Google Gemini 2.5 generative AI model. It hosts an "Agent Loop" that continuously parses the user's natural language, determines the required sequence of API calls (Tools), executes them against a mocked standard delivery system database (DynamoDB), and synthesizes a final natural language response.

**Key capabilities include:**
- Autonomous cart management and checkout.
- Automated coupon evaluation and application based on mathematical maximization.
- Strict enforcement of negative constraints (Allergies, Dislikes, Blacklisted Restaurants).
- Advanced order tracking and autonomous refund processing for user-reported issues.

## 1.3 AIM & OBJECTIVE OF THE PROJECT

**Primary Aim**:  
To conceptualize, design, and develop a fully functional web-based AI Agent capable of orchestrating the entire food delivery lifecycle through natural language processing and autonomous tool execution.

**Core Objectives**:
1. **To integrate an LLM with deterministic software tools**: Allow the generative AI model to safely interact with backend APIs (e.g., `search_menu`, `checkout_cart`, `track_order`) without hallucinating data.
2. **To implement Controlled Autonomy**: Design a confidence-scoring algorithm allowing the AI to act independently (Full Auto) or seek human permission (Conservative) based on user preference settings and historical ordering data.
3. **To ensure robust dietary safety**: Create a rigid system prompt and filtering mechanism that guarantees the AI will never retrieve or order items that violate the user's allergy profile.
4. **To engineer a real-time reactive UI**: Utilize Server-Sent Events (SSE) to stream the AI's internal thought process step-by-step to the React frontend, maintaining high system transparency and user trust.
5. **To deploy a scalable cloud backend**: Utilize AWS DynamoDB Enhanced client for persistence and design the application to be Serverless/AWS Lambda compatible.

---

# 2 SYSTEM ANALYSIS

## 2.1 MOTIVATION

The motivation for this project stems from the intersection of AI Agent functionality and daily consumer technology. With the release of capable models like Google's Gemini, the tech industry is shifting from "AI as a Copilot" (assisting with tasks) to "AI as an Agent" (executing tasks autonomously). 

Food delivery is a perfect vertical to demonstrate agentic capabilities because it contains well-defined API boundaries (search, cart, payment, tracking) but requires complex reasoning (balancing budget, taste, discounts, and constraints). If an AI can reliably order food by understanding abstract commands like *"Get my usual pizza, but I only have ₹400 in my wallet"*, it proves that deterministic APIs and probabilistic LLMs can be cleanly bridged. This system aims to be a prototype for the future of ambient computing, where apps have no buttons, only a conversation bar.

## 2.2 LITERATURE STUDY

During the initial phase of the project, significant research was conducted into Agentic Frameworks and conversational AI:

1. **Large Language Models and Function Calling**: Traditional LLMs were limited to text generation. The introduction of "Function Calling" (or Tool Calling) allowed models to output structured JSON matching predefined schemas. This project utilizes the `gemini-2.5-flash` model's native capability to halt generation, request local execution of a tool (e.g., `check_wallet`), and ingest the result to continue reasoning.
2. **ReAct Prompting (Reasoning and Acting)**: The backend architecture is heavily inspired by the ReAct methodology proposed by Yao et al. (2022). The `AgentLoopService` operates in a while-loop, repeatedly feeding tool outputs back into the LLM until a final conversational response is achieved.
3. **Spring AI Framework**: A relatively new addition to the Spring ecosystem, Spring AI abstracts the complexities of prompt engineering, history management, and LLM provider APIs. Studying the Spring AI documentation was crucial to implementing the OpenAI-adapter which interfaces with Google Gemini.

---

# 3 DESIGN: ANALYSIS & METHODOLOGY

## 3.1 REQUIREMENT ANALYSIS

Identifying system requirements was critical before initiating the development phase. The system requires near-zero latency in UI updates and robust handling of API rate limits from the LLM provider.

### 3.1.1 System Requirements

**3.1.1.1 Hardware Requirements**
- **Development**: Apple Silicon Mac (M-series) or PC with Intel i5/i7 (8th Gen+).
- **RAM**: Minimum 16GB (due to running React + Vite server, Spring Boot Tomcat server, and heavily IDE).
- **Storage**: Minimum 500MB free disk space.

**3.1.1.2 Software Requirements**
- **Backend Language**: Java Subsystem 17
- **Backend Framework**: Spring Boot 3.4.1, Spring Data REST, Spring Web
- **AI Framework**: Spring AI (`spring-ai-openai-spring-boot-starter`)
- **Frontend Library**: React version 19.2.0, Vite 7.3.1
- **Styling**: Vanilla CSS with modern Glassmorphism aesthetics
- **Database SDK**: AWS SDK for Java (DynamoDB Enhanced) `2.25.0`
- **Security**: JWT implementation (`jjwt-api`, `jjwt-impl`) and Google OAuth 2.0.

**3.1.1.3 Software Environment**
- **IDE**: IntelliJ IDEA (Backend), VS Code / Cursor (Frontend)
- **Runtime Environment**: Node.js (v18+), Maven (v3.8+)
- **Cloud/External Dependencies**: Active Internet Connection, Google Gemini API Key, AWS Credentials (for DB).

## 3.2 SYSTEM ARCHITECTURE

The C.A.F.E. system utilizes a modern Client-Server API-driven architecture separated into two distinct monoliths, communicating primarily over REST and Server-Sent Events (SSE).

1. **Presentation Layer (React Vite SPA)**
   State management is handled via native React Hooks context. The UI features a split-pane layout: a Chat Canvas on the left and a State Panel on the right (displaying the Cart, Wallet, map, and AI Agent Traces).
2. **Application Layer (Spring Boot)**
   The Controller layer intercepts requests and passes them to the `AgentLoopService`. The application layer maintains the conversation history natively and trims it dynamically to avoid exceeding the Gemini model's token limits.
3. **Agent Logic Layer**
   The core innovation. Upon receiving user text, the `AgentLoopService` builds a highly complex system prompt containing the user's dynamically injected profile (budget, allergies, cuisine preferences). It invokes the Gemini API. If Gemini requests a tool, `AgentTool.java` dynamically routes the call to the mock `FakeSwiggyController`.
4. **Data Access Layer (AWS DynamoDB)**
   Entities like `UserProfile`, `UserWallet`, `OrderHistoryEntry`, and `ChatSession` are serialized and persisted to NoSQL document tables.

## 3.3 MODULE SPECIFICATION

The system is logically divided into the following key modules:

**1. Authentication & Security Module**
- Utilizes JWT (JSON Web Tokens) to secure the REST APIs.
- Incorporates a React Google OAuth Login flow, validating the JWT securely on the Spring Boot backend to issue an internal application token.

**2. User Profiling Module**
- Manages the `UserProfile` entity. Users define their Autonomy Level (Full Auto, Balanced, Conservative).
- Stores the arrays of Allergies (e.g., Peanuts, Dairy) and Dislikes.
- Calculates dynamic confidence metrics based on past ordering behavior.

**3. Tool Registration & Dispatcher Module (`AgentTool.java`)**
This module defines the JSON schemas sent to the LLM. Key tools include:
- `search_menu(query, userId)`: Filters out allergies recursively.
- `check_wallet(userId)`: Verifies balance before purchasing.
- `evaluate_coupons(restaurantId, itemName)`: Mathematically ranks the best discount.
- `add_to_cart()`, `checkout_cart()`: Executes state mutations.
- `track_order(orderId)`: Generates real-time map data.

**4. Conversational Loop Module (`AgentLoopService.java`)**
- Manages an exponential backoff retry mechanism for API rate limits (HTTP 429).
- Uses atomic integer key rotation across multiple Gemini API keys to bypass free-tier limitations (15 Requests Per Minute).
- Streams execution traces back to the frontend using the `SseEmitter` class.

## 3.4 TIMELINE CHART

| Phase | Time Estimate | Description |
| :--- | :--- | :--- |
| 1. Project Planning and Setup | 2 Weeks | Defining architecture, establishing Git repos, creating Spring Boot & Vite skeletons. |
| 2. Backend Data Models | 2 Weeks | Designing DynamoDB schemas and implementing User Profile/Wallet services. |
| 3. Agent Tool Implementation | 3 Weeks | Writing the `AgentTool.java` dispatcher and mocking the Swiggy APIs. |
| 4. LLM Integration & Loop | 3 Weeks | Integrating Spring AI, setting up the `AgentLoopService`, and prompt engineering the system prompt. |
| 5. Frontend Chat UI | 2 Weeks | Building the React components, message bubbles, and styling the interface. |
| 6. System Integration & Testing | 2 Weeks | Connecting SSE streams, testing edge cases (e.g., rate limits, API key rotation). |
| 7. Documentation and Reporting | 1 Week | Writing the final project report and gathering screenshots. |
| **Total Duration** | **15 Weeks** | |

## 3.5 UML DIAGRAMS

**(Textual Representation due to format constraints)**

**Use Case Diagram:**
- **Actor (User)** interacts with the System to: Send Message, View Agent Trace, View Delivery Map, View Cart.
- **System (Agent)** interacts with Subsystems to: Evaluate Coupons, Place Order, Deduct Wallet, Track GPS.

**Sequence Diagram (Agent Loop):**
1. User -> React Client: "Order pizza"
2. React Client -> AgentController: POST /api/agent/chat (stream)
3. AgentController -> AgentLoopService: init SseEmitter
4. AgentLoopService -> Gemini API: generateContent (with prompt + history)
5. Gemini API -> AgentLoopService: functionCall "search_menu"
6. AgentLoopService -> React Client: SSE Event (Trace: Searching menu)
7. AgentLoopService -> AgentTool: executeTool("search_menu")
8. AgentTool -> FakeSwiggy: return filtered JSON items
9. AgentLoopService -> Gemini API: generateContent (with function result)
10. Gemini API -> AgentLoopService: Text Response "I found pizza, ordering..."
11. AgentLoopService -> React Client: SSE Event (Result: Text Response)

---

# 4 IMPLEMENTATION

## 4.1 SYSTEM FLOW

The entry point of the application is the `OnboardingPage.css / .jsx` screen where users authenticate. Upon login, they enter the main Chat Interface. 

When a user submits a textual or voice-transcribed prompt, the frontend opens an `EventSource` connection to the Backend SSE endpoint. The backend initializes a loop. It pulls the user's `UserProfile` from the database, converts preferences into an LLM-understandable format, and injects it into a massive `system_instruction`.

The system flow loops up to a maximum of 10 times internally:
- If the LLM requests a tool, the application executes the Java method dynamically based on the string name in the JSON response, packages the result back into a JSON structure, and pings the LLM again.
- While looping, it emits `trace` events to the frontend, which are caught and displayed in the right-side "Agent Brain" UI panel.

## 4.2 DATA FLOW DIAGRAMS

The data flows securely between the React App and the Spring Application securely authenticated via the `Authorization: Bearer <token>` header.
The Conversation History is maintained within the Backend memory (via `ConversationHistoryService.java`), preventing tampering from the client side. The architecture enforces that the user cannot directly execute an order; the User can only speak to the Agent, and the AI Agent possesses the exclusive server-side authority to mutate the Database (e.g. deduct wallet balance). This guarantees security.

## 4.3 RESULTS

### 4.3.1 System Testing

Rigorous testing was conducted to validate the core features:
- **Constraints Testing**: When configured with a "Peanut" allergy, prompts like "Order a Peanut Sundae" successfully resulted in the AI actively refusing the command and suggesting alternatives, proving the effectiveness of the system prompt.
- **Resilience Testing**: The `AgentLoopService` correctly caught `HTTP 429 Too Many Requests` errors from the Gemini API and successfully executed an Thread.sleep() exponential backoff and API key rotation mechanism without failing the user's request.
- **Autonomy Testing**: Under "Conservative" autonomy, the AI asked "Should I proceed to checkout?". Under "Full Auto", the AI parsed the sentence, added the item to the cart, applied the coupon, and placed the order in a single click lifecycle.

### 4.3.2 Relevant Screenshots

*(The following screenshots would demonstrate the chat canvas on the left displaying the AI's natural language responses, and the frosted-glass technical details panel on the right rendering the JSON outputs of the tool executions in real-time.)*
- Fig 1: Login & Onboarding Screen
- Fig 2: Main Chat Canvas with User Query
- Fig 3: Agent Brain View showing function calls (`search_menu`, `check_wallet`)
- Fig 4: Simulated Delivery Tracking Map integration.

---

# CONCLUSION AND FUTURE WORK

The C.A.F.E. (Controlled Autonomous Food Engine) project successfully demonstrates that Large Language Models can be securely bridged with deterministic software APIs to create fully autonomous software agents. By prioritizing conversational interfaces over traditional GUIs, we drastically reduce cognitive load and simplify complex workflows like coupon evaluations and strict dietary filtering.

The incorporation of the React 19 frontend paired with the hyper-resilient Spring Boot backend establishes a highly premium, scalable, and stable platform.

**Future Work:**
1. **Multi-Modal Input**: Future iterations could allow users to upload an image of a dish they see on social media, using Gemini's vision capabilities to identify the food and order the nearest equivalent automatically.
2. **True Voice Integration**: Implementing real-time WebSockets with a speech-to-text API to allow hands-free driving-mode ordering.
3. **Micro-agent Architecture**: Splitting the singular "Agent Brain" into specialized sub-agents (e.g., a dedicated "Pricing Negotiator Agent" and a "Dietary Specialist Agent") to improve isolated prompt performance.

---

# REFERENCES

[1] Yao, S. et al. (2022). "ReAct: Synergizing Reasoning and Acting in Language Models". arXiv preprint.  
[2] Google DeepMind. (2024). "Gemini 1.5 and 2.5 API Documentation". Google AI for Developers.  
[3] Spring Framework Documentation. (2024). "Spring AI Reference Guide". Spring.io.  
[4] Amazon Web Services. (2023). "DynamoDB Enhanced Client for Java". AWS Documentation.  

---

# APPENDIX

## SOURCE CODE 

### 1. `AgentLoopService.java` (Core LLM Orchestration)

```java
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

@Service
public class AgentLoopService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKeyConfig;

    @Autowired private AgentTool agentTool;
    @Autowired private ConversationHistoryService historyService;
    @Autowired private UserProfileService userProfileService;

    // ... (RestClient and Constants setup) ...

    public void runAgentLoopStreaming(String userId, String question, SseEmitter emitter) {
        long loopStartTime = System.currentTimeMillis();
        AgentResponse response = new AgentResponse();
        List<AgentResponse.TraceStep> trace = new ArrayList<>();

        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            String systemPrompt = buildSystemPrompt(userId, profile);
            historyService.addUserMessage(userId, question);

            int iterations = 0;
            while (iterations < 10) {
                iterations++;

                Map<String, Object> requestBody = buildGeminiRequest(userId, systemPrompt);
                String jsonBody = mapper.writeValueAsString(requestBody);
                
                // REST POST to Gemini GenerateContent Endpoint with exponential backoff and Key Rotation logic ...
                String rawResponse = performApiCall(jsonBody);

                JsonNode root = mapper.readTree(rawResponse);
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
                            System.currentTimeMillis() - toolStart
                    );
                    trace.add(traceStep);

                    // Stream trace back to Frontend immediately via Server-Sent Events
                    emitter.send(SseEmitter.event().name("trace").data(mapper.writeValueAsString(traceStep)));
                    historyService.addFunctionCall(userId, toolName, toolArgs);
                    historyService.addFunctionResponse(userId, toolName, parsedResult);
                    continue; // Loop again, giving model the tool output.
                }

                if (firstPart.has("text")) {
                    String aiText = firstPart.path("text").asText();
                    response.message = aiText;
                    emitter.send(SseEmitter.event().name("result").data(mapper.writeValueAsString(response)));
                    emitter.complete();
                    return;
                }
                break;
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
```

### 2. `AgentTool.java` (Tool Specifications)

```java
package com.project.agent_brain_service;
// Imports...

@Service
public class AgentTool {

    @Autowired private FakeSwiggyController swiggy;
    @Autowired private UserProfileService userProfileService;

    public String executeTool(String name, Map<String, Object> args) {
        try {
            return switch (name) {
                case "search_menu" -> toolSearchMenu(args);
                case "check_wallet" -> toolCheckWallet(args);
                case "evaluate_coupons" -> toolEvaluateCoupons(args);
                case "place_order" -> toolPlaceOrder(args);
                case "add_to_cart" -> toolAddToCart(args);
                case "checkout_cart" -> toolCheckoutCart(args);
                // ... (Other cases)
                default -> mapper.writeValueAsString(Map.of("error", "Unknown tool: " + name));
            };
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String toolSearchMenu(Map<String, Object> args) throws Exception {
        String query = getStringArg(args, "query", "");
        String userId = getStringArg(args, "userId", "user_123");

        // Allergy logic encapsulation
        UserProfile profile = userProfileService.getUserProfile(userId);
        String allergies = String.join(",", profile.preferences.allergies);
        String blacklist = String.join(",", profile.preferences.blacklistedRestaurants);

        var results = swiggy.searchFood(query, allergies, blacklist);
        return mapper.writeValueAsString(Map.of("results", results, "count", results.size()));
    }

    public List<Map<String, Object>> getFunctionDeclarations() {
        List<Map<String, Object>> declarations = new ArrayList<>();
        declarations.add(makeDeclaration(
                "search_menu",
                "Search for food items across all restaurants. Automatically filters out items matching the user's allergies and excludes blacklisted restaurants.",
                orderedMap(
                        "query", propString("Food keyword to search for"),
                        "userId", propString("The user ID to apply allergy/blacklist filters for")
                ),
                List.of("query", "userId")
        ));
        // Additional function declaration JSON schemas...
        return declarations;
    }
}
```

### 3. React Frontend Initialization (`package.json`)
```json
{
  "name": "frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@react-oauth/google": "^0.13.4",
    "jwt-decode": "^4.0.0",
    "react": "^19.2.0",
    "react-dom": "^19.2.0",
    "recharts": "^3.8.0"
  }
}
```
*(End of Report)*
