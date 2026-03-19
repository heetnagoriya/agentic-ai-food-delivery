package com.project.agent_brain_service.security;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
