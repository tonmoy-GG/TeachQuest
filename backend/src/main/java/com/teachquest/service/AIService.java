package com.teachquest.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@Service
public class AIService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> evaluateShortAnswer(String question, String studentAnswer, String correctAnswer) {
        if (studentAnswer == null || studentAnswer.trim().length() < 3) {
            Map<String, Object> result = new HashMap<>();
            result.put("score", 0);
            result.put("feedback", "Your answer is too short. Please provide a more detailed explanation.");
            return result;
        }

        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty()
                && !geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            try {
                return evaluateWithGemini(question, studentAnswer, correctAnswer);
            } catch (Exception e) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("score", 0);
                errorResult.put("feedback", "AI Evaluation Error: " + e.getMessage());
                return errorResult;
            }
        }

        Map<String, Object> noKeyResult = new HashMap<>();
        noKeyResult.put("score", 0);
        noKeyResult.put("feedback", "AI Evaluation is currently offline. Please provide a Gemini API key.");
        return noKeyResult;
    }

    public List<Map<String, Object>> evaluateMultipleAnswers(List<Map<String, String>> answers) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            return answers.stream().map(a -> {
                Map<String, Object> r = new HashMap<>();
                r.put("score", 0);
                r.put("feedback", "AI Offline");
                return r;
            }).collect(java.util.stream.Collectors.toList());
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
                    + geminiApiKey;

            StringBuilder itemsPrompt = new StringBuilder();
            for (int i = 0; i < answers.size(); i++) {
                Map<String, String> item = answers.get(i);
                itemsPrompt.append(String.format("Item %d:\nQuestion: %s\nStudent Answer: %s\nExpected Concept: %s\n\n",
                        i + 1, item.get("question"), item.get("userAnswer"), item.get("correctAnswer")));
            }

            String prompt = "Act as a STRICT expert technical tutor. You MUST carefully evaluate each student answer for correctness.\n\n"
                    + itemsPrompt.toString()
                    + "CRITICAL RULES:\n"
                    + "1. Evaluate each item separately and carefully.\n"
                    + "2. If Student Answer is unrelated to the Question, score is 0. Do NOT be lenient.\n"
                    + "3. If Student Answer is factually WRONG, score is 0-20. Explicitly state the error.\n"
                    + "4. If Student Answer is partially correct with some wrong elements, score is 30-60.\n"
                    + "5. Only give 85-100 score if answer is fully correct with all key concepts.\n"
                    + "6. IMPORTANT: Feedback must identify WHAT is wrong, not what is right.\n"
                    + "7. Return ONLY a valid JSON list: [{\"score\": number (0-100), \"feedback\": \"string\"}, ...]\n"
                    + "8. Match the order of the items provided exactly.\n"
                    + "9. Do NOT be overly generous with scores. Be strict about correctness.";

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", Collections.singletonList(part));
            requestBody.put("contents", Collections.singletonList(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map contentObj = (Map) candidate.get("content");
                    List parts = (List) contentObj.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        String resultText = (String) ((Map) parts.get(0)).get("text");
                        int start = resultText.indexOf("[");
                        int end = resultText.lastIndexOf("]");
                        if (start != -1 && end != -1) {
                            resultText = resultText.substring(start, end + 1);
                            return objectMapper.readValue(resultText, List.class);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Bulk AI Evaluation failed: " + e.getMessage());
        }
        throw new RuntimeException("Empty or invalid response from Gemini");
    }

    private Map<String, Object> evaluateWithGemini(String question, String studentAnswer, String correctAnswer)
            throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
                + geminiApiKey;

        String prompt = String.format(
                "Act as a strict expert technical tutor. You MUST carefully evaluate if the student's answer is correct or wrong.\n\n"
                        + "Question: %s\n" + "Expected/Correct Answer: %s\n" + "Student's Answer: %s\n\n" + "CRITICAL RULES:\n"
                        + "1. FIRST: Check if the student answer contains the key concepts/keywords from the expected answer. If NO key concepts match, score must be 0-10.\n"
                        + "2. If Student Answer is completely unrelated to the Question, return score 0 with feedback 'This answer is completely incorrect'.\n"
                        + "3. If Student Answer is factually WRONG or contradicts the expected answer, return score 0-15 with specific correction.\n"
                        + "4. If Student Answer has some correct elements but is incomplete/partially wrong, return score 30-60.\n"
                        + "5. If Student Answer is fully correct with all key concepts, return score 85-100.\n"
                        + "6. IMPORTANT: Do NOT give high scores to partially correct answers. Be strict.\n"
                        + "7. Feedback must be honest. If wrong, explicitly state what was wrong.\n\n"
                        + "Return ONLY valid JSON: {\"score\": number (0-100), \"feedback\": \"string with specific feedback\"}",
                question, (correctAnswer != null ? correctAnswer : "General technical knowledge"), studentAnswer);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", Collections.singletonList(part));
        requestBody.put("contents", Collections.singletonList(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map contentObj = (Map) candidate.get("content");
                    List parts = (List) contentObj.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        String resultText = (String) ((Map) parts.get(0)).get("text");
                        int start = resultText.indexOf("{");
                        int end = resultText.lastIndexOf("}");
                        if (start != -1 && end != -1) {
                            resultText = resultText.substring(start, end + 1);
                            return objectMapper.readValue(resultText, Map.class);
                        }
                    }
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            Map<String, Object> limitResult = new HashMap<>();
            limitResult.put("score", 0); // Changed to 0 if busy to be realistic
            limitResult.put("feedback",
                    "The AI is currently busy. Please try a shorter quiz or check again later.");
            return limitResult;
        } catch (Exception e) {
            throw new RuntimeException("AI Error: " + e.getMessage());
        }
        throw new RuntimeException("Empty or invalid response from Gemini");
    }

    public List<Map<String, Object>> generateQuizQuestions(String course, String difficulty, String type, int count)
            throws Exception {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            throw new Exception("AI Key is missing. Please configure gemini.api.key in application.properties");
        }

        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
                + geminiApiKey;

        String prompt = String.format(
                "Act as a computer science professor. Generate exactly %d %s questions for the course '%s' at '%s' difficulty level.\n\n"
                        +
                        "Rules:\n" +
                        "1. For MCQ: Return 4 options (A, B, C, D) and the correct answer.\n" +
                        "2. For SHORT_QUESTION: Return a descriptive question that requires a conceptual explanation.\n"
                        +
                        "3. Ensure questions are relevant to the Bangladesh CSE curriculum standard.\n" +
                        "4. Return ONLY a JSON list of objects. No markdown backticks, no extra text.\n\n" +
                        "JSON Format for MCQ:\n" +
                        "[{\"question\": \"...\", \"options\": [\"A) ...\", \"B) ...\", \"C) ...\", \"D) ...\"], \"correctAnswer\": \"A\", \"explanation\": \"...\"}]\n\n"
                        +
                        "JSON Format for SHORT_QUESTION:\n" +
                        "[{\"question\": \"...\", \"expectedContent\": \"keywords or key concepts expected in answer\", \"explanation\": \"The ideal answer summary\"}]\n\n"
                        +
                        "Return JSON only.",
                count, type, course, difficulty);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", Collections.singletonList(part));
        requestBody.put("contents", Collections.singletonList(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map contentObj = (Map) candidate.get("content");
                    List parts = (List) contentObj.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        String resultText = (String) ((Map) parts.get(0)).get("text");

                        int start = resultText.indexOf("[");
                        int end = resultText.lastIndexOf("]");
                        if (start != -1 && end != -1) {
                            resultText = resultText.substring(start, end + 1);
                            return objectMapper.readValue(resultText, List.class);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("AI Question Generation Error: " + e.getMessage());
        }
        throw new RuntimeException("Failed to generate questions from AI.");
    }

    public String generatePerformanceFeedback(int scorePercentage, int total, String topic) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return String.format("You scored %d%% in %s. Keep practicing to improve!", scorePercentage, topic);
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key="
                    + geminiApiKey;

            // Calculate correct answers from percentage
            int correctAnswers = (int) Math.round((scorePercentage / 100.0) * total);

            String prompt = String.format(
                    "Act as an encouraging academic mentor. A student completed a quiz about '%s'.\n"
                            + "They answered %d questions correctly out of %d total questions.\n"
                            + "Their percentage score is %d%%.\n\n"
                            + "Provide a brief (2-3 sentences) performance analysis.\n"
                            + "- If percentage is 80%% or above: Praise their excellent performance.\n"
                            + "- If percentage is 50-79%%: Encourage them and note room for improvement.\n"
                            + "- If percentage is below 50%%: Be supportive and suggest reviewing the material.\n\n"
                            + "IMPORTANT: Do NOT confuse the percentage score with the number of correct answers.\n"
                            + "Return ONLY the feedback text, no extra formatting.",
                    topic, correctAnswers, total, scorePercentage);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", Collections.singletonList(part));
            requestBody.put("contents", Collections.singletonList(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map contentObj = (Map) candidate.get("content");
                    List parts = (List) contentObj.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) ((Map) parts.get(0)).get("text");
                    }
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return String.format("You scored %d%% in %s. Good effort!", scorePercentage, topic);
    }
}
