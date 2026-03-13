package com.project.agent_brain_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.agent_brain_service.controller.FakeSwiggyController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Defines the 7 tools the AI agent can autonomously call.
 *
 * Tools:
 *   1. search_menu       - Find food items (with allergy/blacklist filtering)
 *   2. check_wallet      - Check user's wallet balance and history
 *   3. place_order       - Place an order (deducts money)
 *   4. get_reviews       - Read customer reviews for an item
 *   5. evaluate_coupons  - Compare available coupons and find best deal
 *   6. rank_restaurants  - 🆕 Rank search results by user preference, rating, budget fit
 *   7. track_order       - 🆕 Get real-time delivery tracking for an order
 */
@Service
public class AgentTool {

    @Autowired
    private FakeSwiggyController swiggy;

    @Autowired
    private UserProfileService userProfileService;

    private final ObjectMapper mapper = new ObjectMapper();

    // ==================== TOOL DISPATCHER ====================

    public String executeTool(String name, Map<String, Object> args) {
        try {
            return switch (name) {
                case "search_menu" -> toolSearchMenu(args);
                case "check_wallet" -> toolCheckWallet(args);
                case "place_order" -> toolPlaceOrder(args);
                case "get_reviews" -> toolGetReviews(args);
                case "evaluate_coupons" -> toolEvaluateCoupons(args);
                case "rank_restaurants" -> toolRankRestaurants(args);
                case "track_order" -> toolTrackOrder(args);
                case "cancel_order" -> toolCancelOrder(args);
                case "report_issue" -> toolReportIssue(args);
                default -> mapper.writeValueAsString(Map.of("error", "Unknown tool: " + name));
            };
        } catch (Exception e) {
            try {
                return mapper.writeValueAsString(Map.of("error", e.getMessage()));
            } catch (Exception ex) {
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }
    }

    // ==================== TOOL IMPLEMENTATIONS ====================

    /**
     * 🆕 UPDATED: Now passes user's allergies and blacklisted restaurants to filter results.
     */
    private String toolSearchMenu(Map<String, Object> args) throws Exception {
        String query = getStringArg(args, "query", "");
        String userId = getStringArg(args, "userId", "user_123");

        // Get user profile to extract allergies and blacklisted restaurants
        UserProfile profile = userProfileService.getUserProfile(userId);
        String allergies = String.join(",", profile.preferences.allergies);
        String blacklist = String.join(",", profile.preferences.blacklistedRestaurants);

        var results = swiggy.searchFood(query, allergies, blacklist);
        return mapper.writeValueAsString(Map.of(
                "results", results,
                "count", results.size(),
                "query", query,
                "filters_applied", Map.of(
                        "allergies_filtered", profile.preferences.allergies,
                        "restaurants_excluded", profile.preferences.blacklistedRestaurants
                )
        ));
    }

    private String toolCheckWallet(Map<String, Object> args) throws Exception {
        String userId = getStringArg(args, "userId", "user_123");
        double balance = swiggy.getWalletBalance(userId);
        UserWallet wallet = swiggy.getWallet(userId);
        List<String> recentTransactions = new ArrayList<>();
        if (wallet != null && wallet.transactionHistory.size() > 0) {
            int start = Math.max(0, wallet.transactionHistory.size() - 3);
            recentTransactions = wallet.transactionHistory.subList(start, wallet.transactionHistory.size());
        }
        return mapper.writeValueAsString(Map.of(
                "userId", userId,
                "balance", balance,
                "recent_transactions", recentTransactions
        ));
    }

    private String toolPlaceOrder(Map<String, Object> args) throws Exception {
        String restaurantId = getStringArg(args, "restaurantId", "");
        String item = getStringArg(args, "item", "");
        String userId = getStringArg(args, "userId", "user_123");
        String couponCode = getStringArg(args, "couponCode", "");

        String result = swiggy.placeOrder(restaurantId, item, userId, couponCode);
        return mapper.writeValueAsString(Map.of("result", result));
    }

    private String toolGetReviews(Map<String, Object> args) throws Exception {
        String restaurantId = getStringArg(args, "restaurantId", "");
        String itemName = getStringArg(args, "itemName", "");
        var reviews = swiggy.getItemReviews(restaurantId, itemName);
        return mapper.writeValueAsString(Map.of(
                "reviews", reviews,
                "count", reviews.size(),
                "item", itemName
        ));
    }

    private String toolEvaluateCoupons(Map<String, Object> args) throws Exception {
        String restaurantId = getStringArg(args, "restaurantId", "");
        String itemName = getStringArg(args, "itemName", "");
        var evaluations = swiggy.evaluateCoupons(restaurantId, itemName);
        return mapper.writeValueAsString(Map.of(
                "coupons", evaluations,
                "count", evaluations.size(),
                "item", itemName,
                "restaurant_id", restaurantId
        ));
    }

    /**
     * 🆕 Rank search results using the user's preferences, ratings, budget, and coupon potential.
     */
    private String toolRankRestaurants(Map<String, Object> args) throws Exception {
        String userId = getStringArg(args, "userId", "user_123");
        String query = getStringArg(args, "query", "");

        var rankedResults = swiggy.rankResults(userId, query);
        return mapper.writeValueAsString(Map.of(
                "ranked_results", rankedResults,
                "count", rankedResults.size(),
                "query", query,
                "note", "Results are scored and sorted by: user preference (35%), rating (25%), budget fit (25%), coupon potential (15%)"
        ));
    }

    /**
     * 🆕 Get real-time delivery tracking for an active order.
     */
    private String toolTrackOrder(Map<String, Object> args) throws Exception {
        String orderId = getStringArg(args, "orderId", "");
        var tracking = swiggy.trackOrder(orderId);
        return mapper.writeValueAsString(tracking);
    }

    /**
     * 🆕 Cancel an order with realistic refund rules based on order status.
     */
    private String toolCancelOrder(Map<String, Object> args) throws Exception {
        String orderId = getStringArg(args, "orderId", "");
        String userId = getStringArg(args, "userId", "user_123");
        String reason = getStringArg(args, "reason", "User requested cancellation");
        var result = swiggy.cancelOrderWithRefund(orderId, userId, reason);
        return mapper.writeValueAsString(result);
    }

    /**
     * 🆕 Report an issue with a delivered/in-transit order.
     */
    private String toolReportIssue(Map<String, Object> args) throws Exception {
        String orderId = getStringArg(args, "orderId", "");
        String userId = getStringArg(args, "userId", "user_123");
        String issueType = getStringArg(args, "issueType", "");
        var result = swiggy.reportIssue(orderId, userId, issueType);
        return mapper.writeValueAsString(result);
    }

    // ==================== FUNCTION DECLARATIONS (for Gemini API) ====================

    public List<Map<String, Object>> getFunctionDeclarations() {
        List<Map<String, Object>> declarations = new ArrayList<>();

        // 1. search_menu (🆕 updated with userId)
        declarations.add(makeDeclaration(
                "search_menu",
                "Search for food items across all restaurants. Automatically filters out items matching the user's allergies and excludes blacklisted restaurants. Returns items with names, prices, ratings, stock levels, cuisine type, and available coupon count.",
                orderedMap(
                        "query", propString("Food keyword to search for (e.g., 'pizza', 'burger', 'veg', 'spicy'). Use empty string for all items."),
                        "userId", propString("The user ID to apply allergy/blacklist filters for (e.g., 'user_123')")
                ),
                List.of("query", "userId")
        ));

        // 2. check_wallet
        declarations.add(makeDeclaration(
                "check_wallet",
                "Check a user's wallet balance and recent transaction history. ALWAYS check this before placing an order to ensure sufficient funds.",
                orderedMap(
                        "userId", propString("The user ID to check wallet for (e.g., 'user_123')")
                ),
                List.of("userId")
        ));

        // 3. place_order
        declarations.add(makeDeclaration(
                "place_order",
                "Place a food order from a restaurant. This DEDUCTS money from the wallet. Only call this when you are confident the user wants to order AND has sufficient funds. Apply the best coupon code to save money. After placing, the order can be tracked using track_order.",
                orderedMap(
                        "restaurantId", propString("Restaurant ID from search results (e.g., 'res_1')"),
                        "item", propString("Exact item name to order (must match search results exactly)"),
                        "userId", propString("User ID placing the order"),
                        "couponCode", propString("Coupon code to apply for discount. Use empty string if no coupon.")
                ),
                List.of("restaurantId", "item", "userId")
        ));

        // 4. get_reviews
        declarations.add(makeDeclaration(
                "get_reviews",
                "Get customer reviews for a specific menu item. Use this to assess food quality, especially when comparing similar items from different restaurants.",
                orderedMap(
                        "restaurantId", propString("Restaurant ID (e.g., 'res_1')"),
                        "itemName", propString("Exact menu item name (e.g., 'Pizza')")
                ),
                List.of("restaurantId", "itemName")
        ));

        // 5. evaluate_coupons
        declarations.add(makeDeclaration(
                "evaluate_coupons",
                "Evaluate all available coupons for a restaurant item. Returns each coupon's discount amount, final price, and whether it's applicable. Use this to find the best deal before ordering.",
                orderedMap(
                        "restaurantId", propString("Restaurant ID (e.g., 'res_1')"),
                        "itemName", propString("Menu item name to evaluate coupons for (e.g., 'Pizza')")
                ),
                List.of("restaurantId", "itemName")
        ));

        // 🆕 6. rank_restaurants
        declarations.add(makeDeclaration(
                "rank_restaurants",
                "Rank food search results based on user preferences, restaurant ratings, budget fit, and coupon potential. Use this AFTER search_menu when multiple options are found, to pick the BEST option for this specific user. Returns scored and sorted results with reasoning.",
                orderedMap(
                        "userId", propString("User ID to personalize ranking for (e.g., 'user_123')"),
                        "query", propString("Food keyword to search and rank (same as search_menu query)")
                ),
                List.of("userId", "query")
        ));

        // 7. track_order
        declarations.add(makeDeclaration(
                "track_order",
                "Get real-time delivery tracking for an active order. Returns current status (PLACED/PREPARING/OUT_FOR_DELIVERY/DELIVERED), estimated time remaining, delivery partner info, and GPS coordinates. Use this when the user asks about their order status.",
                orderedMap(
                        "orderId", propString("The order ID to track (e.g., 'ORD-A1B2C')")
                ),
                List.of("orderId")
        ));

        // 🆕 8. cancel_order
        declarations.add(makeDeclaration(
                "cancel_order",
                "Cancel an active order. Refund depends on order status: PLACED=full refund, PREPARING=50% refund (restaurant already cooking), OUT_FOR_DELIVERY=cannot cancel, DELIVERED=cannot cancel (use report_issue instead). Always explain the refund outcome to the user.",
                orderedMap(
                        "orderId", propString("The order ID to cancel (e.g., 'ORD-A1B2C')"),
                        "userId", propString("User ID requesting the cancellation"),
                        "reason", propString("Reason for cancellation (e.g., 'Changed my mind', 'Ordered wrong item')")
                ),
                List.of("orderId", "userId")
        ));

        // 🆕 9. report_issue
        declarations.add(makeDeclaration(
                "report_issue",
                "Report a problem with a delivered or in-transit order. Issue types: WRONG_ITEM (full refund), MISSING_ITEM (full refund), NEVER_DELIVERED (full refund), COLD_FOOD (50% refund), BAD_QUALITY (50% refund). Use this when user complains about a received order. For orders not yet delivered, use cancel_order instead.",
                orderedMap(
                        "orderId", propString("The order ID with the issue"),
                        "userId", propString("User ID reporting the issue"),
                        "issueType", propString("Type of issue: WRONG_ITEM, MISSING_ITEM, COLD_FOOD, NEVER_DELIVERED, or BAD_QUALITY")
                ),
                List.of("orderId", "userId", "issueType")
        ));

        return declarations;
    }

    // ==================== JSON HELPERS ====================

    private Map<String, Object> makeDeclaration(String name, String description,
                                                 Map<String, Object> properties, List<String> required) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "OBJECT");
        params.put("properties", properties);
        params.put("required", required);

        Map<String, Object> decl = new LinkedHashMap<>();
        decl.put("name", name);
        decl.put("description", description);
        decl.put("parameters", params);
        return decl;
    }

    private Map<String, Object> propString(String description) {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("type", "STRING");
        prop.put("description", description);
        return prop;
    }

    /** Creates a LinkedHashMap preserving insertion order (important for readability) */
    private LinkedHashMap<String, Object> orderedMap(Object... keysAndValues) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private String getStringArg(Map<String, Object> args, String key, String defaultVal) {
        Object val = args.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}
