package com.project.agent_brain_service.controller;

import com.project.agent_brain_service.UserProfile;
import com.project.agent_brain_service.UserWallet;
import com.project.agent_brain_service.security.AuthResponse;
import com.project.agent_brain_service.security.JwtUtil;
import com.project.agent_brain_service.security.LoginRequest;
import com.project.agent_brain_service.service.DynamoDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private DynamoDbService dbService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest authRequest) {
        String userId = authRequest.getUsername(); 
        
        // For prototyping, we check if user exists in DB. If yes, generate token.
        // In real app, we'd check password hash vs DB.
        UserProfile profile = dbService.getUser(userId);

        if (profile != null) {
            String jwt = jwtUtil.generateToken(userId);
            return ResponseEntity.ok(new AuthResponse(jwt, userId));
        } else {
            return ResponseEntity.status(401).body("User not found");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest authRequest) {
        String userId = authRequest.getUsername();
        if (dbService.getUser(userId) != null) {
            return ResponseEntity.badRequest().body("User already exists");
        }

        // Create user
        UserProfile profile = new UserProfile(userId);
        dbService.saveUser(profile);
        
        // Create initial wallet
        UserWallet wallet = new UserWallet(userId, 500.0); // Welcome bonus
        dbService.saveWallet(wallet);

        String jwt = jwtUtil.generateToken(userId);
        return ResponseEntity.ok(new AuthResponse(jwt, userId));
    }
}
