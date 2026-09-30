package com.teachquest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PracticeQuizGenerationServiceTest {
    @Test
    void normalizesModelOptionLabelsAndUsesAuditedAnswerKey() {
        OllamaChatClient chatClient = mock(OllamaChatClient.class);
        when(chatClient.chat(anyString(), any(), anyInt())).thenReturn(
                "{\"questions\":[{\"type\":\"MCQ\",\"question\":\"Which is a tree traversal?\","
                        + "\"options\":[{\"id\":\"1\",\"text\":\"In-order\"},{\"id\":\"2\",\"text\":\"Hashing\"},"
                        + "{\"id\":\"3\",\"text\":\"Sorting\"},{\"id\":\"4\",\"text\":\"Compression\"}],"
                        + "\"correctOptionId\":\"2\",\"expectedAnswer\":null,\"explanation\":\"An in-order traversal visits nodes recursively.\",\"difficultyNote\":\"core\"}],\"modelNote\":\"generated\"}");
        when(chatClient.chat(anyString(), any())).thenReturn(
                "{\"answers\":[{\"questionOrder\":1,\"correctOptionId\":\"A\",\"rationale\":\"In-order is a tree traversal.\"}]}");
        PracticeQuizGenerationService service = new PracticeQuizGenerationService(chatClient, new ObjectMapper());

        PracticeQuizGenerationService.GeneratedQuestion question = service
                .generate("Computer Science", "Trees", 1, 1, "MCQ_ONLY").get(0);

        assertEquals(List.of("A", "B", "C", "D"), question.options().stream()
                .map(PracticeQuizGenerationService.GeneratedOption::id).toList());
        assertEquals("A", question.correctOptionId());
        assertNull(question.expectedAnswer());
        assertEquals("In-order is a tree traversal.", question.explanation());
    }

        @Test
        void usesShortAnswerExplanationWhenTheModelOmitsExpectedAnswer() {
                OllamaChatClient chatClient = mock(OllamaChatClient.class);
                when(chatClient.chat(anyString(), any(), anyInt())).thenReturn(
                        "{\"questions\":[{\"type\":\"MCQ\",\"question\":\"Which traversal visits root between subtrees?\","
                                                + "\"options\":[{\"id\":\"A\",\"text\":\"Inorder\"},{\"id\":\"B\",\"text\":\"Preorder\"},"
                                                + "{\"id\":\"C\",\"text\":\"Postorder\"},{\"id\":\"D\",\"text\":\"Level order\"}],"
                                                + "\"correctOptionId\":\"D\",\"expectedAnswer\":null,\"explanation\":\"Inorder visits left, root, then right.\"}]}",
                        "{\"questions\":[{\"type\":\"SHORT_ANSWER\",\"question\":\"Name the root node's parent.\","
                                + "\"options\":[{\"id\":\"A\",\"text\":\"None\"}],\"correctOptionId\":\"A\","
                                + "\"expectedAnswer\":null,\"explanation\":\"The root node has no parent.\"}]}");
                when(chatClient.chat(anyString(), any())).thenReturn(auditResponse(1));
                PracticeQuizGenerationService service = new PracticeQuizGenerationService(chatClient, new ObjectMapper());

                List<PracticeQuizGenerationService.GeneratedQuestion> questions = service
                                .generate("Computer Science", "Trees", 1, 2, "BOTH");

                PracticeQuizGenerationService.GeneratedQuestion shortAnswer = questions.get(1);
                assertEquals("SHORT_ANSWER", shortAnswer.type());
                assertEquals("The root node has no parent.", shortAnswer.expectedAnswer());
                assertTrue(shortAnswer.options().isEmpty());
                assertNull(shortAnswer.correctOptionId());
        }

        @Test
        void retriesShortResponsesWithAnExactCountConstraint() {
                OllamaChatClient chatClient = mock(OllamaChatClient.class);
                when(chatClient.chat(anyString(), any(), anyInt())).thenReturn(quizResponse(1), quizResponse(2));
                when(chatClient.chat(anyString(), any())).thenReturn(auditResponse(2));
                PracticeQuizGenerationService service = new PracticeQuizGenerationService(chatClient, new ObjectMapper());

                List<PracticeQuizGenerationService.GeneratedQuestion> questions = service
                                .generate("Computer Science", "Trees", 1, 2, "MCQ_ONLY");

                assertEquals(2, questions.size());
                ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<Object> schemas = ArgumentCaptor.forClass(Object.class);
                ArgumentCaptor<Integer> tokenBudgets = ArgumentCaptor.forClass(Integer.class);
                verify(chatClient, times(2)).chat(prompts.capture(), schemas.capture(), tokenBudgets.capture());
                assertTrue(prompts.getAllValues().get(1).contains("exactly 2 question objects"));
                assertTrue(prompts.getAllValues().get(0).contains("one sentence of at most 20 words"));
                assertEquals(List.of(1536, 1536), tokenBudgets.getAllValues());
                Map<?, ?> schema = (Map<?, ?>) schemas.getValue();
                Map<?, ?> properties = (Map<?, ?>) schema.get("properties");
                Map<?, ?> questionsSchema = (Map<?, ?>) properties.get("questions");
                assertEquals(2, questionsSchema.get("minItems"));
                assertEquals(2, questionsSchema.get("maxItems"));
                Map<?, ?> questionSchema = (Map<?, ?>) questionsSchema.get("items");
                Map<?, ?> questionProperties = (Map<?, ?>) questionSchema.get("properties");
                Map<?, ?> optionsSchema = (Map<?, ?>) questionProperties.get("options");
                assertEquals(4, optionsSchema.get("minItems"));
                assertEquals(4, optionsSchema.get("maxItems"));
                Map<?, ?> optionSchema = (Map<?, ?>) optionsSchema.get("items");
                Map<?, ?> optionProperties = (Map<?, ?>) optionSchema.get("properties");
                assertEquals(100, ((Map<?, ?>) optionProperties.get("text")).get("maxLength"));
        }

                @Test
                void retriesMcqsThatHaveFewerThanFourOptions() {
                        OllamaChatClient chatClient = mock(OllamaChatClient.class);
                        when(chatClient.chat(anyString(), any(), anyInt()))
                                        .thenReturn(threeOptionQuizResponse(), quizResponse(1));
                        when(chatClient.chat(anyString(), any())).thenReturn(auditResponse(1));
                        PracticeQuizGenerationService service = new PracticeQuizGenerationService(chatClient, new ObjectMapper());

                        List<PracticeQuizGenerationService.GeneratedQuestion> questions = service
                                        .generate("Computer Science", "Trees", 1, 1, "MCQ_ONLY");

                        assertEquals(4, questions.get(0).options().size());
                        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
                        verify(chatClient, times(2)).chat(prompts.capture(), any(), anyInt());
                        assertTrue(prompts.getAllValues().get(1).contains("MCQs require exactly four options"));
                        assertTrue(prompts.getAllValues().get(1).contains("exactly four options labeled A, B, C, and D"));
                }

        @Test
        void regeneratesRepeatedQuestionTextWithSpecificRetryGuidance() {
                OllamaChatClient chatClient = mock(OllamaChatClient.class);
                when(chatClient.chat(anyString(), any(), anyInt())).thenReturn(duplicateQuizResponse(), quizResponse(2));
                when(chatClient.chat(anyString(), any())).thenReturn(auditResponse(2));
                PracticeQuizGenerationService service = new PracticeQuizGenerationService(chatClient, new ObjectMapper());

                List<PracticeQuizGenerationService.GeneratedQuestion> questions = service
                                .generate("Computer Science", "Trees", 1, 2, "MCQ_ONLY");

                assertEquals(2, questions.size());
                ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
                verify(chatClient, times(2)).chat(prompts.capture(), any(), anyInt());
                assertTrue(prompts.getAllValues().get(1).contains("Questions must be distinct"));
                assertTrue(prompts.getAllValues().get(1).contains("different concept or scenario"));
        }

        @Test
        void repairsOnlyRepeatedQuestionsWhenTheFullQuizRetryStillDuplicates() {
                OllamaChatClient chatClient = mock(OllamaChatClient.class);
                when(chatClient.chat(anyString(), any(), anyInt())).thenReturn(
                                duplicateQuizResponse(), duplicateQuizResponse());
                when(chatClient.chat(anyString(), any(), anyInt(), anyDouble())).thenReturn(
                                singleQuestionResponse("What is a binary tree?"), quizResponse(1));
                when(chatClient.chat(anyString(), any())).thenReturn(auditResponse(2));
                PracticeQuizGenerationService service = new PracticeQuizGenerationService(chatClient, new ObjectMapper());

                List<PracticeQuizGenerationService.GeneratedQuestion> questions = service
                                .generate("Computer Science", "Trees", 1, 2, "MCQ_ONLY");

                assertEquals(2, questions.size());
                assertTrue(!questions.get(0).question().equalsIgnoreCase(questions.get(1).question()));
                verify(chatClient, times(2)).chat(anyString(), any(), anyInt());
                ArgumentCaptor<String> repairPrompts = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<Double> temperatures = ArgumentCaptor.forClass(Double.class);
                verify(chatClient, times(2)).chat(repairPrompts.capture(), any(), anyInt(), temperatures.capture());
                assertTrue(repairPrompts.getAllValues().get(0).contains("Do not repeat or paraphrase any of these used stems"));
                assertTrue(repairPrompts.getAllValues().get(0).contains("What is a binary tree?"));
                assertTrue(repairPrompts.getAllValues().get(0).contains("Return only one question"));
                assertTrue(repairPrompts.getAllValues().get(1).contains("Switch to a different topic facet"));
                assertTrue(repairPrompts.getAllValues().get(1).contains("What is a binary tree?"));
                assertEquals(List.of(0.75, 0.75), temperatures.getAllValues());
        }

        private String duplicateQuizResponse() {
                String question = "What is a binary tree?";
                String optionSet = "\"options\":[{\"id\":\"A\",\"text\":\"At most two children\"},"
                                + "{\"id\":\"B\",\"text\":\"Any number of children\"},"
                                + "{\"id\":\"C\",\"text\":\"No root node\"},{\"id\":\"D\",\"text\":\"A sorted array\"}],";
                String first = "{\"type\":\"MCQ\",\"question\":\"" + question + "\"," + optionSet
                                + "\"correctOptionId\":\"A\",\"expectedAnswer\":null,\"explanation\":\"Each node has at most two children.\"}";
                String second = first.replace("Each node has at most two children.", "This is the definition of a binary tree.");
                return "{\"questions\":[" + first + "," + second + "]}";
        }

        private String threeOptionQuizResponse() {
                return "{\"questions\":[{\"type\":\"MCQ\",\"question\":\"What is a binary tree?\","
                                + "\"options\":[{\"id\":\"A\",\"text\":\"At most two children\"},"
                                + "{\"id\":\"B\",\"text\":\"Any number of children\"},"
                                + "{\"id\":\"C\",\"text\":\"A sorted array\"}],\"correctOptionId\":\"A\","
                                + "\"expectedAnswer\":null,\"explanation\":\"A binary tree node has at most two children.\"}]}";
        }

        private String singleQuestionResponse(String stem) {
                return "{\"questions\":[{\"type\":\"MCQ\",\"question\":\"" + stem + "\","
                                + "\"options\":[{\"id\":\"A\",\"text\":\"At most two children\"},"
                                + "{\"id\":\"B\",\"text\":\"Any number of children\"},"
                                + "{\"id\":\"C\",\"text\":\"A sorted array\"},{\"id\":\"D\",\"text\":\"A graph cycle\"}],"
                                + "\"correctOptionId\":\"A\",\"expectedAnswer\":null,\"explanation\":\"Each node has at most two children.\"}]}";
        }

        private String quizResponse(int count) {
                StringBuilder response = new StringBuilder("{\"questions\":[");
                for (int index = 1; index <= count; index++) {
                        if (index > 1) response.append(',');
                        response.append("{\"type\":\"MCQ\",\"question\":\"Tree question ").append(index)
                                        .append("?\",\"options\":[{\"id\":\"A\",\"text\":\"Answer ").append(index)
                                        .append("A\"},{\"id\":\"B\",\"text\":\"Answer ").append(index)
                                        .append("B\"},{\"id\":\"C\",\"text\":\"Answer ").append(index)
                                        .append("C\"},{\"id\":\"D\",\"text\":\"Answer ").append(index)
                                        .append("D\"}],\"correctOptionId\":\"A\",\"expectedAnswer\":null,\"explanation\":\"Reason ")
                                        .append(index).append(".\"}");
                }
                return response.append("]}").toString();
        }

        private String auditResponse(int count) {
                StringBuilder response = new StringBuilder("{\"answers\":[");
                for (int index = 1; index <= count; index++) {
                        if (index > 1) response.append(',');
                        response.append("{\"questionOrder\":").append(index)
                                        .append(",\"correctOptionId\":\"A\",\"rationale\":\"Answer A is correct.\"}");
                }
                return response.append("]}").toString();
        }
}