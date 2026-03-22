package com.project.agent_brain_service.controller;

import com.project.agent_brain_service.UserProfile;
import com.project.agent_brain_service.service.DynamoDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Handles the first-time onboarding wizard.
 * Called after signup to save favourite foods, budget, and allergies.
 */
@RestController
@RequestMapping("/api/onboarding")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class OnboardingController {

    @Autowired
    private DynamoDbService dbService;

    /**
     * GET /api/onboarding/status?userId=xxx
     * Returns whether onboarding is already complete or not.
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus(@RequestParam String userId) {
        UserProfile profile = dbService.getUser(userId);
        boolean complete = (profile != null && profile.onboardingComplete);
        return Map.of("onboardingComplete", complete);
    }

    /**
     * POST /api/onboarding/complete
     * Saves onboarding preferences and marks onboarding as done.
     *
     * Body: {
     *   "userId": "email@example.com",
     *   "favoriteFoods": ["pizza", "biryani"],
     *   "cuisines": ["Italian", "Indian"],
     *   "allergies": ["peanuts"],
     *   "budgetMin": 100,
     *   "budgetMax": 500
     * }
     */
    @PostMapping("/complete")
    @SuppressWarnings("unchecked")
    public Map<String, String> completeOnboarding(@RequestBody Map<String, Object> body) {
        try {
            String userId = (String) body.get("userId");
            UserProfile profile = dbService.getUser(userId);
            if (profile == null) {
                return Map.of("status", "error", "message", "User not found: " + userId);
            }

            // Favourite foods -> likes
            List<String> favFoods = (List<String>) body.getOrDefault("favoriteFoods", List.of());
            List<String> cuisines = (List<String>) body.getOrDefault("cuisines", List.of());
            List<String> allergies = (List<String>) body.getOrDefault("allergies", List.of());
            double budgetMin = ((Number) body.getOrDefault("budgetMin", 0)).doubleValue();
            double budgetMax = ((Number) body.getOrDefault("budgetMax", 1000)).doubleValue();

            if (profile.preferences == null) profile.preferences = new UserProfile.Preferences();
            if (profile.budget == null) profile.budget = new UserProfile.Budget();

            profile.preferences.likes = favFoods;
            profile.preferences.cuisines = cuisines;
            profile.preferences.allergies = allergies;
            profile.budget.rangeMin = budgetMin;
            profile.budget.rangeMax = budgetMax;
            profile.onboardingComplete = true;

            dbService.saveUser(profile);
            return Map.of("status", "ok", "message", "Onboarding complete! Preferences saved.");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
