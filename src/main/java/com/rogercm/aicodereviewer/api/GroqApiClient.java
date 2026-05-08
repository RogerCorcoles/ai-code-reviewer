package com.rogercm.aicodereviewer.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.diagnostic.Logger;
import com.rogercm.aicodereviewer.model.ReviewResult;
import com.rogercm.aicodereviewer.settings.AppSettingsState;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GroqApiClient {

    private static final Logger LOG = Logger.getInstance(GroqApiClient.class);
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String SYSTEM_PROMPT =
            "You are an expert code reviewer. Analyze the provided code and give a structured review covering:\n" +
            "1. **Bugs & Issues** - Any bugs, potential runtime errors, or logical flaws\n" +
            "2. **Code Quality** - Readability, maintainability, and adherence to conventions\n" +
            "3. **Performance** - Any performance concerns or inefficiencies\n" +
            "4. **Best Practices** - Design patterns, SOLID principles, and language best practices\n" +
            "5. **Improvements** - Specific, actionable suggestions\n\n" +
            "Be concise, constructive, and specific. Use markdown formatting.";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;

    public GroqApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public ReviewResult reviewCode(String code, String language) {
        AppSettingsState settings = AppSettingsState.getInstance();
        String apiKey = settings.getApiKey();
        String model = settings.getModel();

        LOG.info("reviewCode: model=" + model + ", language=" + language
                 + ", codeLength=" + code.length()
                 + ", apiKeySet=" + (apiKey != null && !apiKey.isBlank()));

        if (apiKey == null || apiKey.isBlank()) {
            return ReviewResult.error(
                    "API key not configured. Go to Settings → Tools → AI Code Reviewer.");
        }

        try {
            String requestBody = buildRequestBody(code, language, model);
            LOG.info("POST " + API_URL);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("Response status: " + response.statusCode());

            if (response.statusCode() != 200) {
                String errMsg = extractErrorMessage(response.body());
                LOG.warn("Non-200 response: " + response.statusCode() + " — " + errMsg);
                return ReviewResult.error("API error " + response.statusCode() + ": " + errMsg);
            }

            String content = extractContent(response.body());
            LOG.info("Extracted content length: " + content.length());
            return ReviewResult.success(content);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.warn("Request interrupted");
            return ReviewResult.error("Request was interrupted.");
        } catch (Exception ex) {
            LOG.error("HTTP call failed", ex);
            return ReviewResult.error("Failed to reach Groq API: " + ex.getMessage());
        }
    }

    private String buildRequestBody(String code, String language, String model) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 2048);
        root.put("temperature", 0.3);

        ArrayNode messages = root.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        String langHint = (language != null && !language.isBlank()) ? " (" + language + ")" : "";
        userMsg.put("content", "Please review the following code" + langHint + ":\n\n```\n" + code + "\n```");

        return MAPPER.writeValueAsString(root);
    }

    private String extractContent(String responseBody) throws Exception {
        JsonNode root = MAPPER.readTree(responseBody);
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode msg = root.path("error").path("message");
            if (!msg.isMissingNode()) return msg.asText();
        } catch (Exception ignored) {
        }
        return responseBody.length() > 300 ? responseBody.substring(0, 300) + "…" : responseBody;
    }
}
