package com.project.agent_brain_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * User management endpoints.
 *
 * CHANGE: Reset now also clears conversation history (not just user profile).
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ConversationHistoryService conversationHistoryService;

    @GetMapping("/profile")
    public UserProfile getProfile(@RequestParam String userId) {
        return userProfileService.getUserProfile(userId);
    }

    static class ProfileUpdateRequest {
        public String languagePreference;
        public List<String> allergies;
        public String autonomyLevel;
    }

    @PostMapping("/profile")
    public UserProfile updateProfile(@RequestParam String userId, @RequestBody ProfileUpdateRequest req) {
        userProfileService.updateUserPreferences(userId, req.languagePreference, req.allergies, req.autonomyLevel);
        return userProfileService.getUserProfile(userId);
    }

    /**
     * Resets both user profile AND conversation history.
     * Called by the Dashboard's "Reset System Memory" button.
     */
    @DeleteMapping("/reset")
    public String resetProfile(@RequestParam String userId) {
        userProfileService.clearUserProfile(userId);
        conversationHistoryService.clearHistory(userId);
        return "🧹 Memory Cleared: Profile + Conversation History wiped for " + userId;
    }
}