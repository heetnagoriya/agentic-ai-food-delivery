package com.project.agent_brain_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserProfileService userProfileService;

    // Endpoint to Simulate Buying Food
    // URL: http://localhost:8080/user/buy?userId=user_123&amount=200&cuisine=Chinese
    @PostMapping("/buy")
    public UserProfile buyFood(
            @RequestParam String userId,
            @RequestParam double amount,
            @RequestParam String cuisine) {
        
        // 1. Update the Memory
        userProfileService.updateUserStats(userId, amount, cuisine);
        
        // 2. Return the UPDATED profile so we can see the math happen
        return userProfileService.getUserProfile(userId);
    }
    
    // Helper to just check profile without buying
    @GetMapping("/profile")
    public UserProfile getProfile(@RequestParam String userId) {
        return userProfileService.getUserProfile(userId);
    }
}