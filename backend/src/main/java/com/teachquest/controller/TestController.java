package com.teachquest.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class TestController {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @GetMapping("/api/test/list-models")
    public String listAvailableModels() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://generativelanguage.googleapis.com/v1/models?key=" + geminiApiKey;
            String response = restTemplate.getForObject(url, String.class);
            return response;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
