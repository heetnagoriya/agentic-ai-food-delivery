package com.project.agent_brain_service.security;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String otp; // Required for email verification
}

