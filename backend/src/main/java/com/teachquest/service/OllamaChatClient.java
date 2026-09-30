package com.teachquest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OllamaChatClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String model;

    public OllamaChatClient(RestTemplateBuilder builder,
                            @Value("${teachquest.practice.ollama.base-url:http://localhost:11434}") String baseUrl,
                            @Value("${teachquest.practice.ollama.model:qwen2.5:1.5b}") String model) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
    }

    public String chat(String prompt, Object responseFormat) {
        return chat(prompt, responseFormat, null, 0.2);
    }

    public String chat(String prompt, Object responseFormat, Integer maxTokens) {
        return chat(prompt, responseFormat, maxTokens, 0.2);
    }

    public String chat(String prompt, Object responseFormat, Integer maxTokens, double temperature) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("stream", false);
        request.put("format", responseFormat);
        request.put("messages", java.util.List.of(Map.of("role", "user", "content", prompt)));
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", temperature);
        options.put("repeat_penalty", 1.15);
        if (maxTokens != null) options.put("num_predict", maxTokens);
        request.put("options", options);

        try {
            Map<?, ?> response = restTemplate.postForObject(
                    baseUrl + "/api/chat",
                    new org.springframework.http.HttpEntity<>(request, headers()),
                    Map.class);
            if (response == null || !(response.get("message") instanceof Map<?, ?> message)
                    || !(message.get("content") instanceof String content) || content.isBlank()) {
                throw new IllegalStateException("Ollama returned an empty response.");
            }
            return content;
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "The local AI service is unavailable. Start Ollama and ensure the configured model is installed.",
                    exception);
        }
    }

    private org.springframework.http.HttpHeaders headers() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}