package com.project.agent_brain_service.controller;

import com.project.agent_brain_service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simulates a real food delivery API (like Swiggy/Zomato).
 * Provides: Restaurants, Menus, Wallets, Orders, Reviews, Coupons, Surge Pricing,
 *           🆕 Allergy Filtering, 🆕 Multi-Restaurant Ranking, 🆕 Delivery Tracking.
 */
@RestController
@RequestMapping("/fake-swiggy")
public class FakeSwiggyController {

    private static final List<Restaurant> RESTAURANTS = new ArrayList<>();
    private static final Map<String, Order> ACTIVE_ORDERS = new HashMap<>();
    private static final Map<String, UserWallet> WALLETS = new HashMap<>();

    @Autowired
    private UserProfileService userProfileService;

    // Surge Pricing State
    private static boolean SURGE_ACTIVE = false;
    private static final double SURGE_MULTIPLIER = 1.5;

    // 🆕 Fake coordinates for restaurants and user locations
    private static final Map<String, double[]> RESTAURANT_COORDS = new HashMap<>();
    private static final double[] USER_DEFAULT_COORDS = {22.5645, 72.9280}; // Default user location (Anand)

    static {
        // --- 💰 WALLETS ---
        WALLETS.put("user_123", new UserWallet("user_123", 2500.0));  // Rich user
        WALLETS.put("user_456", new UserWallet("user_456", 100.0));   // Budget user

        // ══════════════════════════════════════════════════════════
        //   🍕 PIZZA RESTAURANTS (Luigi's vs Pizza Hut)
        // ══════════════════════════════════════════════════════════

        // --- 🏪 RESTAURANT 1: Luigi's Italian (Premium, high-rated) ---
        Restaurant r1 = new Restaurant("res_1", "Luigi's Italian", true, 4.8, "Downtown");
        r1.addItem("Pizza", 600.0, "Italian", List.of("Cheesy", "Heavy", "Premium"), 50);
        r1.menu.get(0).addReview("Alice", 5.0, "Best pizza in town! The cheese pull is real.", "Positive");
        r1.menu.get(0).addReview("Bob", 3.0, "Good but way too oily. Felt heavy afterwards.", "Neutral");
        r1.menu.get(0).addReview("Charlie", 2.0, "Arrived cold and the crust was soggy.", "Negative");

        r1.addItem("Pasta", 450.0, "Italian", List.of("Light", "Veg"), 5);
        r1.menu.get(1).addReview("Dave", 5.0, "Perfectly al dente. The white sauce is amazing.", "Positive");

        r1.addCoupon("LUIGI60", 60.0, 0, 120.0, 199.0, "60% off up to ₹120 on orders above ₹199");
        r1.addCoupon("LUIGI30", 30.0, 0, 200.0, 399.0, "30% off up to ₹200 on orders above ₹399");

        // --- 🏪 RESTAURANT 4: Pizza Hut (Budget-friendly, decent quality) ---
        Restaurant r4 = new Restaurant("res_4", "Pizza Hut", true, 4.1, "Sector 7");
        r4.addItem("Pizza", 350.0, "Italian", List.of("Cheesy", "Veg"), 80);
        r4.menu.get(0).addReview("Nisha", 4.0, "Good value for money. Not gourmet but hits the spot.", "Positive");
        r4.menu.get(0).addReview("Raj", 3.5, "Decent pizza. Cheese could be better quality.", "Neutral");
        r4.menu.get(0).addReview("Sita", 4.5, "Fast delivery and hot pizza. My go-to for budget meals!", "Positive");

        r4.addItem("Pasta", 250.0, "Italian", List.of("Light", "Veg", "Budget"), 40);
        r4.menu.get(1).addReview("Vikram", 3.0, "Average pasta. Nothing special but affordable.", "Neutral");

        r4.addItem("Garlic Bread", 150.0, "Italian", List.of("Veg", "Snack", "Light"), 60);
        r4.menu.get(2).addReview("Anita", 4.0, "Crispy and buttery. Great as a side!", "Positive");

        r4.addCoupon("PHut40", 40.0, 0, 80.0, 199.0, "40% off up to ₹80 on orders above ₹199");
        r4.addCoupon("PHutFLAT", 0, 50.0, 0, 299.0, "Flat ₹50 off on orders above ₹299");

        // ══════════════════════════════════════════════════════════
        //   🍔 BURGER RESTAURANTS (Burger King vs McDonald's)
        // ══════════════════════════════════════════════════════════

        // --- 🏪 RESTAURANT 2: Burger King (Spicy, bold flavors) ---
        Restaurant r2 = new Restaurant("res_2", "Burger King", true, 4.2, "Uptown");
        r2.addItem("Burger", 350.0, "Fast Food", List.of("Spicy", "Non-Veg"), 100);
        r2.menu.get(0).addReview("Eve", 4.0, "Spicy means SPICY! Watch out if you can't handle heat.", "Positive");
        r2.menu.get(0).addReview("Frank", 1.0, "Stale bun. Not worth it.", "Negative");

        r2.addItem("Fries", 80.0, "Fast Food", List.of("Light", "Veg"), 200);
        r2.menu.get(1).addReview("Grace", 4.5, "Crispy and perfectly salted.", "Positive");

        r2.addItem("Chicken Wrap", 220.0, "Fast Food", List.of("Non-Veg", "Light"), 80);
        r2.menu.get(2).addReview("Henry", 4.0, "Good value for money, filling enough.", "Positive");

        r2.addCoupon("BK175", 0, 175.0, 0, 350.0, "Flat ₹175 off on orders above ₹350");
        r2.addCoupon("BK50", 50.0, 0, 100.0, 149.0, "50% off up to ₹100 on orders above ₹149");

        // --- 🏪 RESTAURANT 5: McDonald's (Consistent, family-friendly) ---
        Restaurant r5 = new Restaurant("res_5", "McDonald's", true, 4.4, "Mall Road");
        r5.addItem("Burger", 180.0, "Fast Food", List.of("Mild", "Non-Veg", "Budget"), 150);
        r5.menu.get(0).addReview("Priya", 4.0, "Classic McBurger taste. Consistent as always.", "Positive");
        r5.menu.get(0).addReview("Arjun", 4.5, "Best budget burger in town. Quick service too.", "Positive");
        r5.menu.get(0).addReview("Meera", 3.5, "Not as big as I'd like but tastes good.", "Neutral");

        r5.addItem("Fries", 120.0, "Fast Food", List.of("Light", "Veg", "Crispy"), 200);
        r5.menu.get(1).addReview("Karthik", 5.0, "Golden, crispy perfection. Best fries period.", "Positive");
        r5.menu.get(1).addReview("Divya", 4.0, "Consistent quality. Never disappointed.", "Positive");

        r5.addItem("McFlurry", 150.0, "Dessert", List.of("Sweet", "Veg", "Cold"), 100);
        r5.menu.get(2).addReview("Sneha", 5.0, "Perfect on a hot day! Love the Oreo one.", "Positive");

        r5.addCoupon("MC99", 0, 30.0, 0, 99.0, "Flat ₹30 off on orders above ₹99");
        r5.addCoupon("MCMEAL", 25.0, 0, 75.0, 249.0, "25% off up to ₹75 on orders above ₹249");

        // ══════════════════════════════════════════════════════════
        //   🥘 SOUTH INDIAN / INDIAN (Spice Garden vs Dosa Plaza)
        // ══════════════════════════════════════════════════════════

        // --- 🏪 RESTAURANT 3: Spice Garden (Traditional, premium South Indian) ---
        Restaurant r3 = new Restaurant("res_3", "Spice Garden", true, 4.6, "Midtown");
        r3.addItem("Masala Dosa", 120.0, "South Indian", List.of("Veg", "Crispy"), 30);
        r3.menu.get(0).addReview("Isha", 5.0, "Authentic taste. The chutney is surprisingly spicy.", "Positive");
        r3.menu.get(0).addReview("Jay", 4.0, "Crispy outside, soft inside. Classic.", "Positive");

        r3.addItem("Biryani", 280.0, "Indian", List.of("Non-Veg", "Spicy", "Heavy"), 25);
        r3.menu.get(1).addReview("Kumar", 5.0, "Restaurant-quality biryani delivered hot.", "Positive");
        r3.menu.get(1).addReview("Lakshmi", 4.5, "Fragrant rice with perfectly cooked meat.", "Positive");

        r3.addItem("Idli Sambar", 90.0, "South Indian", List.of("Veg", "Light", "Healthy"), 60);

        r3.addCoupon("SPICE50", 50.0, 0, 80.0, 99.0, "50% off up to ₹80 on orders above ₹99");
        r3.addCoupon("NEWSPICE", 0, 30.0, 0, 99.0, "Flat ₹30 off on orders above ₹99");

        // --- 🏪 RESTAURANT 6: Dosa Plaza (Quick, budget South Indian) ---
        Restaurant r6 = new Restaurant("res_6", "Dosa Plaza", true, 4.3, "Station Road");
        r6.addItem("Masala Dosa", 80.0, "South Indian", List.of("Veg", "Crispy", "Budget"), 50);
        r6.menu.get(0).addReview("Ramesh", 3.5, "Quick and cheap. Not the best dosa but fills you up.", "Neutral");
        r6.menu.get(0).addReview("Sunita", 4.0, "Surprisingly good for the price! Crispy and fresh.", "Positive");

        r6.addItem("Idli Sambar", 60.0, "South Indian", List.of("Veg", "Light", "Budget", "Healthy"), 80);
        r6.menu.get(1).addReview("Gopal", 4.5, "Soft fluffy idlis. Sambar is phenomenal.", "Positive");

        r6.addItem("Biryani", 180.0, "Indian", List.of("Non-Veg", "Spicy", "Budget"), 40);
        r6.menu.get(2).addReview("Fatima", 3.0, "Okay biryani. Rice was a bit undercooked.", "Neutral");
        r6.menu.get(2).addReview("Ravi", 4.0, "Great value for ₹180. Generous portions.", "Positive");

        r6.addCoupon("DOSA20", 20.0, 0, 40.0, 59.0, "20% off up to ₹40 on orders above ₹59");
        r6.addCoupon("DOSANEW", 0, 15.0, 0, 79.0, "Flat ₹15 off on orders above ₹79");

        // ══════════════════════════════════════════════════════════
        //   Register all restaurants
        // ══════════════════════════════════════════════════════════
        RESTAURANTS.add(r1);  // Luigi's Italian
        RESTAURANTS.add(r2);  // Burger King
        RESTAURANTS.add(r3);  // Spice Garden
        RESTAURANTS.add(r4);  // Pizza Hut
        RESTAURANTS.add(r5);  // McDonald's
        RESTAURANTS.add(r6);  // Dosa Plaza

        // Restaurant coordinates (fake - around Anand, Gujarat)
        RESTAURANT_COORDS.put("res_1", new double[]{22.5726, 72.9290}); // Luigi's - Downtown
        RESTAURANT_COORDS.put("res_2", new double[]{22.5780, 72.9350}); // Burger King - Uptown
        RESTAURANT_COORDS.put("res_3", new double[]{22.5700, 72.9310}); // Spice Garden - Midtown
        RESTAURANT_COORDS.put("res_4", new double[]{22.5660, 72.9260}); // Pizza Hut - Sector 7
        RESTAURANT_COORDS.put("res_5", new double[]{22.5750, 72.9330}); // McDonald's - Mall Road
        RESTAURANT_COORDS.put("res_6", new double[]{22.5690, 72.9270}); // Dosa Plaza - Station Road
    }

    // ================== SURGE CONTROL ==================

    @PostMapping("/surge/on")
    public String enableSurge() { SURGE_ACTIVE = true; return "⛈️ SURGE ENABLED (1.5x Prices)"; }

    @PostMapping("/surge/off")
    public String disableSurge() { SURGE_ACTIVE = false; return "☀️ SURGE DISABLED (Normal Prices)"; }

    @GetMapping("/surge/status")
    public boolean getSurgeStatus() { return SURGE_ACTIVE; }

    // ================== SEARCH (🆕 with fuzzy matching + allergy & blacklist filtering) ==================

    // Common food spelling aliases (query variation → canonical name)
    private static final Map<String, String> FOOD_ALIASES = new HashMap<>();
    static {
        FOOD_ALIASES.put("sambhar", "sambar");
        FOOD_ALIASES.put("sambhaar", "sambar");
        FOOD_ALIASES.put("sambaar", "sambar");
        FOOD_ALIASES.put("naan", "nan");
        FOOD_ALIASES.put("biriyani", "biryani");
        FOOD_ALIASES.put("briyani", "biryani");
        FOOD_ALIASES.put("bryani", "biryani");
        FOOD_ALIASES.put("piza", "pizza");
        FOOD_ALIASES.put("pizzaa", "pizza");
        FOOD_ALIASES.put("berger", "burger");
        FOOD_ALIASES.put("burgar", "burger");
        FOOD_ALIASES.put("burgur", "burger");
        FOOD_ALIASES.put("dosha", "dosa");
        FOOD_ALIASES.put("idly", "idli");
        FOOD_ALIASES.put("idle", "idli");
        FOOD_ALIASES.put("garlik", "garlic");
        FOOD_ALIASES.put("macflurry", "mcflurry");
        FOOD_ALIASES.put("mc flurry", "mcflurry");
    }

    /**
     * Search food with fuzzy matching + optional allergy/blacklist filtering.
     *
     * Matching logic (in order of priority):
     *   1. Exact substring match on item name, cuisine, or tags
     *   2. Alias resolution (e.g., "sambhar" → "sambar")
     *   3. Fuzzy match using Levenshtein distance (edit distance ≤ 2 per word)
     */
    @GetMapping("/search")
    public List<Map<String, Object>> searchFood(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String allergies,
            @RequestParam(defaultValue = "") String blacklistedRestaurantIds) {

        List<Map<String, Object>> results = new ArrayList<>();
        String q = query.toLowerCase().trim();

        // Resolve aliases in query (e.g., "idli sambhar" → "idli sambar")
        String resolvedQuery = resolveAliases(q);

        // Parse allergy list
        List<String> allergyList = allergies.isEmpty()
                ? List.of()
                : Arrays.stream(allergies.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .toList();

        // Parse blacklisted restaurant IDs
        List<String> blacklist = blacklistedRestaurantIds.isEmpty()
                ? List.of()
                : Arrays.stream(blacklistedRestaurantIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

        for (Restaurant r : RESTAURANTS) {
            // Skip blacklisted restaurants
            if (blacklist.contains(r.id)) continue;

            for (MenuItem item : r.menu) {
                // Skip items matching allergies (check name, cuisine, tags)
                if (!allergyList.isEmpty()) {
                    boolean hasAllergen = false;
                    for (String allergy : allergyList) {
                        if (item.name.toLowerCase().contains(allergy)
                                || item.cuisine.toLowerCase().contains(allergy)
                                || item.tags.stream().anyMatch(t -> t.toLowerCase().contains(allergy))) {
                            hasAllergen = true;
                            break;
                        }
                    }
                    if (hasAllergen) continue;
                }

                boolean match = q.isEmpty()
                        || fuzzyMatchItem(resolvedQuery, item);

                if (match) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("restaurant", r.name);
                    entry.put("restaurant_id", r.id);
                    entry.put("restaurant_rating", r.rating);
                    entry.put("restaurant_location", r.location);
                    entry.put("is_open", r.isOpen);
                    entry.put("item", item.name);

                    // Apply surge pricing
                    double currentPrice = SURGE_ACTIVE ? item.price * SURGE_MULTIPLIER : item.price;
                    entry.put("price", currentPrice);
                    entry.put("original_price", item.price);
                    entry.put("surge_active", SURGE_ACTIVE);

                    entry.put("stock", item.stockCount);
                    entry.put("tags", item.tags);
                    entry.put("cuisine", item.cuisine);

                    // Average rating from reviews
                    double avgRating = item.reviews.stream().mapToDouble(rv -> rv.rating).average().orElse(0.0);
                    entry.put("rating", Math.round(avgRating * 10.0) / 10.0);
                    entry.put("review_count", item.reviews.size());

                    // Show available coupons count as a hint
                    entry.put("coupons_available", r.coupons.size());

                    results.add(entry);
                }
            }
        }
        return results.size() > 8 ? results.subList(0, 8) : results;
    }

    // ================== FUZZY SEARCH HELPERS ==================

    /**
     * Resolves food aliases in the query string.
     * e.g., "idli sambhar" → "idli sambar"
     */
    private String resolveAliases(String query) {
        String resolved = query;
        for (Map.Entry<String, String> alias : FOOD_ALIASES.entrySet()) {
            resolved = resolved.replace(alias.getKey(), alias.getValue());
        }
        return resolved;
    }

    /**
     * Checks if a query fuzzy-matches a menu item (name, cuisine, or tags).
     * Uses exact substring first, then falls back to per-word Levenshtein distance.
     */
    private boolean fuzzyMatchItem(String query, MenuItem item) {
        String itemName = item.name.toLowerCase();
        String cuisine = item.cuisine.toLowerCase();
        String allTags = item.tags.stream().map(String::toLowerCase).collect(Collectors.joining(" "));
        String searchable = itemName + " " + cuisine + " " + allTags;

        // 1. Exact substring match (fast path)
        if (searchable.contains(query)) return true;

        // 2. Per-word fuzzy match: each query word must fuzzy-match at least one searchable word
        String[] queryWords = query.split("\\s+");
        String[] searchWords = searchable.split("\\s+");

        for (String qWord : queryWords) {
            if (qWord.isEmpty()) continue;
            boolean wordMatched = false;
            for (String sWord : searchWords) {
                // Substring check first
                if (sWord.contains(qWord) || qWord.contains(sWord)) {
                    wordMatched = true;
                    break;
                }
                // Levenshtein distance check (allow up to 2 edits)
                int maxDist = qWord.length() <= 4 ? 1 : 2;
                if (levenshteinDistance(qWord, sWord) <= maxDist) {
                    wordMatched = true;
                    break;
                }
            }
            if (!wordMatched) return false;
        }
        return true;
    }

    /**
     * Computes the Levenshtein (edit) distance between two strings.
     * Operations: insert, delete, substitute — each costs 1.
     */
    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    // ================== 🆕 RANKING (Multi-Restaurant Resolution) ==================

    /**
     * Ranks search results based on user preferences, ratings, budget fit, and coupons.
     * Returns scored and sorted results with reasoning.
     *
     * Score = (W1 × UserRestaurantPref) + (W2 × Rating) + (W3 × BudgetFit) + (W4 × CouponSavings)
     */
    @GetMapping("/rank")
    public List<Map<String, Object>> rankResults(
            @RequestParam String userId,
            @RequestParam(defaultValue = "") String query) {

        UserProfile profile = userProfileService.getUserProfile(userId);

        // First, get filtered search results (with allergy/blacklist)
        String allergies = String.join(",", profile.preferences.allergies);
        String blacklist = String.join(",", profile.preferences.blacklistedRestaurants);
        List<Map<String, Object>> searchResults = searchFood(query, allergies, blacklist);

        // Score each result
        List<Map<String, Object>> scoredResults = new ArrayList<>();
        for (Map<String, Object> result : searchResults) {
            Map<String, Object> scored = new LinkedHashMap<>(result);

            String restaurantId = (String) result.get("restaurant_id");
            double price = (Double) result.get("price");
            double itemRating = result.get("rating") instanceof Number ? ((Number) result.get("rating")).doubleValue() : 0.0;
            double restRating = result.get("restaurant_rating") instanceof Number ? ((Number) result.get("restaurant_rating")).doubleValue() : 0.0;

            // Factor 1: User's restaurant preference (0.0 to 1.0)
            double userPref = profile.restaurantPreferences.getOrDefault(restaurantId, 0.5);

            // Factor 2: Combined rating score (item + restaurant, normalized to 0-1)
            double ratingScore = ((itemRating + restRating) / 2.0) / 5.0;

            // Factor 3: Budget fit (how well price matches user's sweet spot)
            double budgetMid = (profile.budget.rangeMin + profile.budget.rangeMax) / 2.0;
            double budgetFit;
            if (budgetMid <= 0) {
                budgetFit = 0.5; // Unknown budget
            } else {
                double deviation = Math.abs(price - budgetMid) / budgetMid;
                budgetFit = Math.max(0.0, 1.0 - deviation);
            }

            // Factor 4: Coupon potential (approximated by coupon count)
            int couponsAvailable = (Integer) result.getOrDefault("coupons_available", 0);
            double couponScore = Math.min(1.0, couponsAvailable * 0.3);

            // Weighted score
            double totalScore = (0.35 * userPref)
                    + (0.25 * ratingScore)
                    + (0.25 * budgetFit)
                    + (0.15 * couponScore);

            scored.put("ranking_score", Math.round(totalScore * 100.0) / 100.0);

            // Build reasoning
            List<String> reasons = new ArrayList<>();
            if (userPref >= 0.8) reasons.add("⭐ Strong user preference for this restaurant");
            else if (userPref >= 0.6) reasons.add("👍 User has ordered here before");
            if (ratingScore >= 0.8) reasons.add("🏆 Highly rated (" + restRating + "★)");
            if (budgetFit >= 0.7) reasons.add("💰 Within budget sweet spot");
            else if (budgetFit < 0.4) reasons.add("⚠️ Outside typical budget range");
            if (couponsAvailable > 0) reasons.add("🎟️ " + couponsAvailable + " coupons available");
            scored.put("ranking_reasons", reasons);

            scoredResults.add(scored);
        }

        // Sort by score descending
        scoredResults.sort((a, b) -> Double.compare(
                (Double) b.get("ranking_score"),
                (Double) a.get("ranking_score")
        ));

        return scoredResults;
    }

    // ================== REVIEWS ==================

    @GetMapping("/reviews")
    public List<Review> getItemReviews(@RequestParam String restaurantId, @RequestParam String itemName) {
        MenuItem item = getItemDetails(restaurantId, itemName);
        return (item != null) ? item.reviews : new ArrayList<>();
    }

    // ================== COUPON EVALUATION ==================

    @GetMapping("/evaluate-coupons")
    public List<Map<String, Object>> evaluateCoupons(@RequestParam String restaurantId, @RequestParam String itemName) {
        List<Map<String, Object>> evaluations = new ArrayList<>();

        Optional<Restaurant> resOpt = RESTAURANTS.stream().filter(r -> r.id.equals(restaurantId)).findFirst();
        if (resOpt.isEmpty()) return evaluations;

        Restaurant restaurant = resOpt.get();
        MenuItem item = restaurant.menu.stream()
                .filter(i -> i.name.equalsIgnoreCase(itemName)).findFirst().orElse(null);
        if (item == null) return evaluations;

        double currentPrice = SURGE_ACTIVE ? item.price * SURGE_MULTIPLIER : item.price;

        for (Coupon coupon : restaurant.coupons) {
            Map<String, Object> eval = new LinkedHashMap<>();
            eval.put("code", coupon.code);
            eval.put("description", coupon.description);
            eval.put("item_price", currentPrice);

            double discount = coupon.calculateDiscount(currentPrice);
            boolean applicable = discount > 0;

            eval.put("applicable", applicable);
            eval.put("discount", discount);
            eval.put("final_price", applicable ? currentPrice - discount : currentPrice);
            eval.put("savings_percent", applicable ? Math.round(discount / currentPrice * 100) : 0);

            if (!applicable) {
                eval.put("reason", "Minimum order ₹" + coupon.minOrder + " not met (item is ₹" + currentPrice + ")");
            }

            evaluations.add(eval);
        }

        // Sort by highest discount first
        evaluations.sort((a, b) -> Double.compare((Double) b.get("discount"), (Double) a.get("discount")));
        return evaluations;
    }

    // ================== ORDER (with 🆕 delivery tracking init) ==================

    @PostMapping("/order")
    public String placeOrder(@RequestParam String restaurantId,
                             @RequestParam String item,
                             @RequestParam(defaultValue = "user_123") String userId,
                             @RequestParam(defaultValue = "") String couponCode) {

        UserWallet wallet = WALLETS.get(userId);
        if (wallet == null) return "❌ ERROR: User wallet not found for " + userId;

        MenuItem food = getItemDetails(restaurantId, item);
        if (food == null) return "❌ ERROR: Item '" + item + "' not found at restaurant " + restaurantId;

        Restaurant res = RESTAURANTS.stream().filter(r -> r.id.equals(restaurantId)).findFirst().get();

        if (food.stockCount <= 0) return "❌ OUT OF STOCK: " + food.name;

        // Calculate price with surge
        double basePrice = SURGE_ACTIVE ? food.price * SURGE_MULTIPLIER : food.price;

        // Apply coupon if provided
        double discount = 0;
        String couponInfo = "";
        if (couponCode != null && !couponCode.isEmpty()) {
            Optional<Coupon> couponOpt = res.coupons.stream()
                    .filter(c -> c.code.equalsIgnoreCase(couponCode)).findFirst();
            if (couponOpt.isPresent()) {
                discount = couponOpt.get().calculateDiscount(basePrice);
                couponInfo = " | Coupon " + couponCode + " saved ₹" + discount;
            }
        }

        double finalAmount = basePrice - discount;

        // Deduct from wallet
        String reason = food.name + " @ " + res.name
                + (SURGE_ACTIVE ? " (⚡Surge)" : "")
                + couponInfo;

        if (!wallet.deductAmount(finalAmount, reason)) {
            return "❌ PAYMENT DECLINED: Insufficient funds. Balance: ₹" + wallet.balance + ", Required: ₹" + finalAmount;
        }

        // Reduce stock
        food.stockCount--;

        // Create order WITH delivery tracking info + payment details
        double[] resCords = RESTAURANT_COORDS.getOrDefault(restaurantId, new double[]{22.5726, 72.9290});
        int deliveryTime = 5; // 5 minutes for demo speed (instead of 30)
        Order newOrder = new Order(
                food.name + " from " + res.name,
                res.name,
                deliveryTime,
                resCords[0], resCords[1],
                USER_DEFAULT_COORDS[0], USER_DEFAULT_COORDS[1]
        );

        // 🆕 Store payment info for cancellation/refund calculations
        newOrder.userId = userId;
        newOrder.restaurantId = restaurantId;
        newOrder.paidAmount = finalAmount;
        newOrder.discountApplied = discount;
        newOrder.couponCode = couponCode;

        ACTIVE_ORDERS.put(newOrder.orderId, newOrder);

        // Update user learning (cuisine + restaurant preference)
        userProfileService.updateUserStats(userId, finalAmount, food.cuisine);
        userProfileService.updateRestaurantPreference(userId, restaurantId);

        return "✅ ORDER SUCCESS: " + food.name + " from " + res.name
                + " | Order ID: " + newOrder.orderId
                + " | Paid: ₹" + finalAmount
                + (discount > 0 ? " (saved ₹" + discount + " with " + couponCode + ")" : "")
                + " | New Balance: ₹" + wallet.balance
                + " | 🛵 Delivery by " + newOrder.deliveryPartnerName + " (" + newOrder.deliveryPartnerVehicle + ")"
                + " | ETA: ~" + deliveryTime + " mins";
    }

    // ================== 🆕 DELIVERY TRACKING ==================

    /**
     * Returns real-time delivery tracking information for an order.
     * Status auto-progresses based on elapsed time since placement.
     */
    @GetMapping("/track/{orderId}")
    public Map<String, Object> trackOrder(@PathVariable String orderId) {
        Map<String, Object> tracking = new LinkedHashMap<>();

        Order order = ACTIVE_ORDERS.get(orderId);
        if (order == null) {
            tracking.put("error", "Order not found: " + orderId);
            return tracking;
        }

        String currentStatus = order.calculateCurrentStatus();
        order.status = currentStatus; // Update stored status
        double[] partnerPos = order.getDeliveryPartnerPosition();

        tracking.put("order_id", order.orderId);
        tracking.put("item", order.item);
        tracking.put("restaurant", order.restaurantName);
        tracking.put("status", currentStatus);
        tracking.put("estimated_minutes_remaining", order.getEstimatedMinutesRemaining());
        tracking.put("delivery_partner", order.deliveryPartnerName);
        tracking.put("delivery_vehicle", order.deliveryPartnerVehicle);
        tracking.put("placed_at", order.orderTime.toString());

        // GPS coordinates
        Map<String, Object> coordinates = new LinkedHashMap<>();
        coordinates.put("restaurant", Map.of("lat", order.restaurantLat, "lng", order.restaurantLng));
        coordinates.put("user", Map.of("lat", order.userLat, "lng", order.userLng));
        coordinates.put("delivery_partner", Map.of("lat", partnerPos[0], "lng", partnerPos[1]));
        tracking.put("coordinates", coordinates);

        // Status timeline
        List<Map<String, Object>> timeline = new ArrayList<>();
        String[] stages = {"PLACED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED"};
        String[] stageEmojis = {"📝", "👨‍🍳", "🛵", "✅"};
        String[] stageLabels = {"Order Placed", "Preparing your food", "Out for delivery", "Delivered!"};
        boolean reachedCurrent = false;
        for (int i = 0; i < stages.length; i++) {
            Map<String, Object> stage = new LinkedHashMap<>();
            stage.put("stage", stages[i]);
            stage.put("emoji", stageEmojis[i]);
            stage.put("label", stageLabels[i]);
            if (stages[i].equals(currentStatus)) {
                stage.put("status", "current");
                reachedCurrent = true;
            } else if (!reachedCurrent) {
                stage.put("status", "completed");
            } else {
                stage.put("status", "pending");
            }
            timeline.add(stage);
        }
        tracking.put("timeline", timeline);

        return tracking;
    }

    // ================== WORLD STATE (for Dashboard) ==================

    @GetMapping("/world-state")
    public Map<String, Object> getWorldState() {
        // Auto-update order statuses
        for (Order order : ACTIVE_ORDERS.values()) {
            order.status = order.calculateCurrentStatus();
        }

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("wallets", WALLETS);
        state.put("restaurants", RESTAURANTS);
        state.put("active_orders", ACTIVE_ORDERS);
        state.put("surge_active", SURGE_ACTIVE);
        return state;
    }

    // ================== HELPERS ==================

    public MenuItem getItemDetails(String restaurantId, String itemName) {
        Optional<Restaurant> res = RESTAURANTS.stream().filter(r -> r.id.equals(restaurantId)).findFirst();
        if (res.isPresent()) {
            return res.get().menu.stream()
                    .filter(i -> i.name.equalsIgnoreCase(itemName))
                    .findFirst().orElse(null);
        }
        return null;
    }

    public double getWalletBalance(String userId) {
        return WALLETS.containsKey(userId) ? WALLETS.get(userId).balance : 0.0;
    }

    public UserWallet getWallet(String userId) {
        return WALLETS.get(userId);
    }

    public String getLastOrderId() {
        return ACTIVE_ORDERS.keySet().stream().reduce((f, s) -> s).orElse(null);
    }

    public String getOrderStatus(String orderId) {
        Order order = ACTIVE_ORDERS.get(orderId);
        return order != null ? order.calculateCurrentStatus() : "Not Found";
    }

    // ================== 🆕 ORDER CANCELLATION (Realistic Rules) ==================

    /**
     * Cancel an order with realistic refund rules based on current status.
     *
     * Rules:
     *   PLACED            → ✅ Full refund, stock restored
     *   PREPARING          → ⚠️ 50% refund (restaurant already started cooking)
     *   OUT_FOR_DELIVERY   → ❌ Cannot cancel (food is on the way)
     *   DELIVERED          → ❌ Cannot cancel (use report_issue instead)
     *   CANCELLED          → ❌ Already cancelled
     */
    @PostMapping("/cancel")
    public Map<String, Object> cancelOrderWithRefund(
            @RequestParam String orderId,
            @RequestParam(defaultValue = "user_123") String userId,
            @RequestParam(defaultValue = "User requested cancellation") String reason) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order_id", orderId);

        Order order = ACTIVE_ORDERS.get(orderId);
        if (order == null) {
            result.put("success", false);
            result.put("message", "❌ Order not found: " + orderId);
            return result;
        }

        // Get current status (auto-calculated based on time)
        String currentStatus = order.calculateCurrentStatus();
        result.put("order_status_at_cancellation", currentStatus);

        UserWallet wallet = WALLETS.get(userId);

        switch (currentStatus) {
            case "PLACED" -> {
                // ✅ Full refund — restaurant hasn't started yet
                double refundAmount = order.paidAmount;
                order.isCancelled = true;
                order.cancelledAt = java.time.LocalDateTime.now();
                order.cancellationReason = reason;
                order.refundAmount = refundAmount;
                order.status = "CANCELLED";

                // Refund to wallet
                if (wallet != null) {
                    wallet.refund(refundAmount, "Cancellation refund for " + orderId);
                }

                // Restore stock
                String itemName = order.item.split(" from ")[0];
                MenuItem food = getItemDetails(order.restaurantId, itemName);
                if (food != null) food.stockCount++;

                result.put("success", true);
                result.put("refund_type", "FULL_REFUND");
                result.put("refund_amount", refundAmount);
                result.put("message", "✅ Order cancelled successfully! Full refund of ₹" + refundAmount
                        + " credited to wallet. New balance: ₹" + (wallet != null ? wallet.balance : "N/A"));
            }
            case "PREPARING" -> {
                // ⚠️ 50% refund — food preparation has started
                double refundAmount = Math.round(order.paidAmount * 0.5 * 100.0) / 100.0;
                double cancellationCharge = order.paidAmount - refundAmount;
                order.isCancelled = true;
                order.cancelledAt = java.time.LocalDateTime.now();
                order.cancellationReason = reason;
                order.refundAmount = refundAmount;
                order.status = "CANCELLED";

                if (wallet != null) {
                    wallet.refund(refundAmount, "Partial refund (50%) for cancelled " + orderId);
                }

                result.put("success", true);
                result.put("refund_type", "PARTIAL_REFUND");
                result.put("refund_amount", refundAmount);
                result.put("cancellation_charge", cancellationCharge);
                result.put("message", "⚠️ Order cancelled. Restaurant had already started preparing. "
                        + "Partial refund of ₹" + refundAmount + " (50%). "
                        + "Cancellation charge: ₹" + cancellationCharge
                        + ". New balance: ₹" + (wallet != null ? wallet.balance : "N/A"));
            }
            case "OUT_FOR_DELIVERY" -> {
                // ❌ Cannot cancel — food is already on the way
                result.put("success", false);
                result.put("refund_type", "NO_REFUND");
                result.put("message", "❌ Cannot cancel: Your order is already out for delivery by "
                        + order.deliveryPartnerName + " (" + order.deliveryPartnerVehicle + "). "
                        + "If you face issues after delivery, you can report a problem.");
            }
            case "DELIVERED" -> {
                // ❌ Cannot cancel delivered orders
                result.put("success", false);
                result.put("refund_type", "NO_REFUND");
                result.put("message", "❌ Cannot cancel: Order has already been delivered. "
                        + "If there's a problem (wrong item, missing item, quality issue), "
                        + "please use report_issue instead.");
            }
            case "CANCELLED" -> {
                result.put("success", false);
                result.put("message", "❌ Order was already cancelled on " + order.cancelledAt
                        + ". Refund of ₹" + order.refundAmount + " was already processed.");
            }
            default -> {
                result.put("success", false);
                result.put("message", "❌ Unknown order status: " + currentStatus);
            }
        }

        return result;
    }

    // ================== 🆕 ISSUE REPORTING (Post-Delivery Problems) ==================

    /**
     * Report an issue with a delivered order.
     *
     * Issue types and resolutions:
     *   WRONG_ITEM      → Full refund
     *   MISSING_ITEM     → Full refund
     *   NEVER_DELIVERED  → Full refund
     *   COLD_FOOD        → 50% refund
     *   BAD_QUALITY      → 50% refund
     */
    @PostMapping("/report-issue")
    public Map<String, Object> reportIssue(
            @RequestParam String orderId,
            @RequestParam(defaultValue = "user_123") String userId,
            @RequestParam String issueType) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order_id", orderId);
        result.put("issue_type", issueType);

        Order order = ACTIVE_ORDERS.get(orderId);
        if (order == null) {
            result.put("success", false);
            result.put("message", "❌ Order not found: " + orderId);
            return result;
        }

        String currentStatus = order.calculateCurrentStatus();
        if (!currentStatus.equals("DELIVERED") && !currentStatus.equals("OUT_FOR_DELIVERY")) {
            result.put("success", false);
            result.put("message", "❌ Issues can only be reported for delivered or in-transit orders. "
                    + "Current status: " + currentStatus
                    + ". Use cancel_order instead for PLACED/PREPARING orders.");
            return result;
        }

        if (order.hasIssue) {
            result.put("success", false);
            result.put("message", "❌ Issue already reported for this order. Resolution: "
                    + order.issueResolution + ". Refund: ₹" + order.issueRefundAmount);
            return result;
        }

        UserWallet wallet = WALLETS.get(userId);
        double refundAmount = 0;
        String resolution;
        String message;

        switch (issueType.toUpperCase()) {
            case "WRONG_ITEM", "MISSING_ITEM", "NEVER_DELIVERED" -> {
                // Full refund for critical issues
                refundAmount = order.paidAmount;
                resolution = "FULL_REFUND";
                message = "✅ We're sorry for the inconvenience! Full refund of ₹" + refundAmount
                        + " has been credited to your wallet.";
                if (issueType.equalsIgnoreCase("NEVER_DELIVERED")) {
                    message += " We'll also investigate with the delivery partner " + order.deliveryPartnerName + ".";
                }
            }
            case "COLD_FOOD", "BAD_QUALITY" -> {
                // 50% refund for quality issues
                refundAmount = Math.round(order.paidAmount * 0.5 * 100.0) / 100.0;
                resolution = "PARTIAL_REFUND";
                message = "⚠️ We apologize for the quality issue. Partial refund of ₹" + refundAmount
                        + " (50%) has been credited to your wallet. "
                        + "We've flagged this with " + order.restaurantName + ".";
            }
            default -> {
                result.put("success", false);
                result.put("message", "❌ Unknown issue type: " + issueType
                        + ". Valid types: WRONG_ITEM, MISSING_ITEM, COLD_FOOD, NEVER_DELIVERED, BAD_QUALITY");
                return result;
            }
        }

        // Process refund
        if (wallet != null && refundAmount > 0) {
            wallet.refund(refundAmount, "Issue refund (" + issueType + ") for " + orderId);
        }

        // Record issue on order
        order.hasIssue = true;
        order.issueType = issueType.toUpperCase();
        order.issueResolution = resolution;
        order.issueRefundAmount = refundAmount;

        result.put("success", true);
        result.put("resolution", resolution);
        result.put("refund_amount", refundAmount);
        result.put("new_balance", wallet != null ? wallet.balance : "N/A");
        result.put("message", message);

        return result;
    }
}