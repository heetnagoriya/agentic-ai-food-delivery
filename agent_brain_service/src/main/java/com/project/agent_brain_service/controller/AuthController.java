package com.project.agent_brain_service.controller;

import com.project.agent_brain_service.UserProfile;
import com.project.agent_brain_service.UserWallet;
import com.project.agent_brain_service.security.AuthResponse;
import com.project.agent_brain_service.security.JwtUtil;
import com.project.agent_brain_service.security.LoginRequest;
import com.project.agent_brain_service.security.RegisterRequest;
import com.project.agent_brain_service.service.DynamoDbService;
import com.project.agent_brain_service.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuthController {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private DynamoDbService dbService;
    @Autowired private EmailService emailService;

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    // ─── Login ───────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest authRequest) {
        String email = authRequest.getEmail();
        UserProfile profile = dbService.getUser(email);

        if (profile == null) return ResponseEntity.status(401).body("User not found");

        String storedPassword = profile.getPassword();
        boolean passwordMatches;
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            passwordMatches = passwordEncoder.matches(authRequest.getPassword(), storedPassword);
        } else {
            // Legacy plain-text — auto-upgrade on login
            passwordMatches = storedPassword != null && storedPassword.equals(authRequest.getPassword());
            if (passwordMatches) {
                profile.setPassword(passwordEncoder.encode(authRequest.getPassword()));
                dbService.saveUser(profile);
            }
        }

        if (!passwordMatches) return ResponseEntity.status(401).body("Invalid credentials");

        return ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(email), email));
    }

    // ─── Google Login ───────────────────────────────────────────────────
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        try {
            String credential = body.get("credential");
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("594535232360-ng3nalcaoqubb0kdps7ccmnhuns22hao.apps.googleusercontent.com"))
                    .build();
            
            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken != null) {
                Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                UserProfile profile = dbService.getUser(email);
                if (profile == null) {
                    // Auto-register new Google user
                    profile = new UserProfile(email);
                    profile.setName(name);
                    profile.setEmail(email);
                    dbService.saveUser(profile);
                    
                    UserWallet wallet = new UserWallet(email, 0.0);
                    dbService.saveWallet(wallet);
                }

                return ResponseEntity.ok(Map.of("jwt", jwtUtil.generateToken(email), "userId", email, "name", name, "email", email));
            } else {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid Google ID token."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Google Authentication Failed: " + e.getMessage()));
        }
    }

    // ─── Step 1: Send OTP ────────────────────────────────────────────────
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body("Email required");
        try {
            emailService.sendOtp(email.trim().toLowerCase());
            return ResponseEntity.ok(Map.of("status", "sent", "message", "OTP sent to " + email));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send OTP: " + e.getMessage());
        }
    }

    // ─── Step 2: Verify OTP + Register ───────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest authRequest) {
        String email = authRequest.getEmail().trim().toLowerCase();

        // Must pass OTP verification
        if (!emailService.verifyOtp(email, authRequest.getOtp())) {
            return ResponseEntity.status(403).body("Invalid or expired OTP. Please request a new code.");
        }

        if (dbService.getUser(email) != null) return ResponseEntity.badRequest().body("User already exists");

        UserProfile profile = new UserProfile(email);
        profile.setName(authRequest.getName());
        profile.setEmail(email);
        profile.setPassword(passwordEncoder.encode(authRequest.getPassword()));
        dbService.saveUser(profile);

        UserWallet wallet = new UserWallet(email, 500.0);
        dbService.saveWallet(wallet);

        return ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(email), email));
    }
}
