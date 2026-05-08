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
import java.util.Map;

public class GroqApiClient implements CodeReviewClient {

    private static final Logger LOG = Logger.getInstance(GroqApiClient.class);
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String BASE_SYSTEM_PROMPT =
            "You are an expert code reviewer. Analyze the provided code and give a structured review covering:\n"
            + "1. **Bugs & Issues** - Any bugs, potential runtime errors, or logical flaws\n"
            + "2. **Code Quality** - Readability, maintainability, and adherence to conventions\n"
            + "3. **Performance** - Any performance concerns or inefficiencies\n"
            + "4. **Best Practices** - Design patterns, SOLID principles, and language best practices\n"
            + "5. **Improvements** - Specific, actionable suggestions\n\n"
            + "Be concise, constructive, and specific. Use markdown formatting.";

    // Package-private so LanguageTest can assert that every key is reachable via Language.fromExtension().
    static final Map<String, String> LANGUAGE_GUIDANCE = Map.ofEntries(
        Map.entry("Java",
            "For Java specifically: pay attention to SOLID principles, proper exception handling, "
            + "null safety (consider Optional), thread safety, and resource management (try-with-resources)."),
        Map.entry("Kotlin",
            "For Kotlin specifically: highlight idiomatic Kotlin (data classes, extension functions, "
            + "scope functions), null safety operators, and coroutine usage if present."),
        Map.entry("Python",
            "For Python specifically: check PEP 8 compliance, Pythonic idioms, type hint usage, "
            + "proper use of comprehensions, and common anti-patterns like mutable default arguments."),
        Map.entry("JavaScript",
            "For JavaScript specifically: focus on async/await vs Promise chains, potential "
            + "prototype issues, var vs let/const, and equality pitfalls (== vs ===)."),
        Map.entry("TypeScript",
            "For TypeScript specifically: evaluate type safety, proper use of generics, "
            + "avoidance of 'any', and strict null checks."),
        Map.entry("Go",
            "For Go specifically: check idiomatic error handling (errors as values), goroutine "
            + "safety, proper interface design, and defer usage."),
        Map.entry("Rust",
            "For Rust specifically: assess ownership and borrowing correctness, lifetime "
            + "annotations, Result/Option propagation, and any unsafe block justification."),
        Map.entry("SQL",
            "For SQL specifically: evaluate query performance, missing index opportunities, "
            + "potential injection risks, and normalization.")
    );

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;

    public GroqApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public ReviewResult reviewCode(String code, String language) {
        AppSettingsState settings = AppSettingsState.getInstance();
        String apiKey = settings.getApiKey();
        String model  = settings.getModel();

        LOG.info("reviewCode: model=" + model + ", language=" + language
                 + ", lines=" + code.split("\n").length
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

    // ── Request building ──────────────────────────────────────────────────────

    private String buildRequestBody(String code, String language, String model) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 2048);
        root.put("temperature", 0.3);

        ArrayNode messages = root.putArray("messages");

        messages.addObject()
                .put("role", "system")
                .put("content", buildSystemPrompt(language));

        messages.addObject()
                .put("role", "user")
                .put("content", buildUserMessage(code, language));

        return MAPPER.writeValueAsString(root);
    }

    /**
     * Builds the system prompt by appending language-specific guidance to the
     * base prompt when a known language is detected. This steers the model to
     * mention idioms and pitfalls that are actually relevant to the snippet.
     */
    private static String buildSystemPrompt(String language) {
        String guidance = language != null ? LANGUAGE_GUIDANCE.get(language) : null;
        if (guidance == null) return BASE_SYSTEM_PROMPT;
        return BASE_SYSTEM_PROMPT + "\n\n" + guidance;
    }

    /**
     * Formats the user message with a labelled code fence and line count so the
     * model has explicit structural context about what it is reviewing.
     */
    private static String buildUserMessage(String code, String language) {
        int lines = code.split("\n").length;
        String langLabel = (language != null && !language.isBlank()) ? language : "unknown language";
        String fence     = (language != null && !language.isBlank()) ? language.toLowerCase() : "";
        return String.format(
                "Please review this %d-line %s snippet:\n\n```%s\n%s\n```",
                lines, langLabel, fence, code);
    }

    // ── Response parsing ──────────────────────────────────────────────────────

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
