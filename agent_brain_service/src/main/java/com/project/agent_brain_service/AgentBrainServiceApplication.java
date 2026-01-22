package com.project.agent_brain_service;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AgentBrainServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentBrainServiceApplication.class, args);
    }

    // This Bean FORCES the correct URL in code, ignoring the properties file errors.
    @Bean
    public OpenAiApi openAiApi(@Value("${spring.ai.openai.api-key}") String apiKey) {
        // We hardcode the correct Google URL here (No trailing slash!)
        return new OpenAiApi("https://generativelanguage.googleapis.com/v1beta/openai", apiKey);
    }
}