package com.project.agent_brain_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserProfileService userProfileService;

    @GetMapping("/profile")
    public UserProfile getProfile(@RequestParam String userId) {
        return userProfileService.getUserProfile(userId);
    }
    
    // The Dashboard calls THIS to reset memory
    @DeleteMapping("/reset")
    public String resetProfile(@RequestParam String userId) {
        userProfileService.clearUserProfile(userId);
        return "Memory Cleared for " + userId;
    }
}