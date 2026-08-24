package com.rrtechnosoft.lms.service.chatbot;

import com.rrtechnosoft.lms.config.ChatbotProperties;
import com.rrtechnosoft.lms.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calls the OpenAI chat-completions API (or anything wire-compatible with
 * it — Azure OpenAI's compatible route, a self-hosted vLLM/Ollama server,
 * OpenRouter, etc — via app.chatbot.base-url). Real, working HTTP call —
 * not a stub — but only reachable once app.chatbot.enabled=true and
 * app.chatbot.api-key is set (CHATBOT_ENABLED / CHATBOT_API_KEY env vars),
 * the same pattern RazorpayPaymentGatewayService and
 * WhatsAppNotificationChannel already use for other optional external
 * integrations.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.chatbot", name = "enabled", havingValue = "true")
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final ChatbotProperties properties;
    private final RestClient restClient = RestClient.create();

    public OpenAiCompatibleLlmClient(ChatbotProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generateReply(List<ChatTurn> history, String newUserMessage) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("app.chatbot.enabled=true but no api-key configured; falling back to a generic reply");
            return "The AI assistant isn't fully configured yet — an administrator needs to set "
                    + "CHATBOT_API_KEY. In the meantime, try browsing Learning Resources for this topic.";
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", properties.getSystemPrompt()));
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", newUserMessage));

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", messages,
                "temperature", 0.4
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(properties.getBaseUrl() + "/chat/completions")
                    .headers(h -> h.setBearerAuth(properties.getApiKey()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return extractContent(response);
        } catch (RestClientException e) {
            log.error("Chatbot LLM call failed: {}", e.getMessage());
            throw new ApiException("The AI assistant is temporarily unavailable. Please try again shortly.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) {
            throw new ApiException("Empty response from the AI provider", HttpStatus.BAD_GATEWAY);
        }
        List<Object> choices = (List<Object>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new ApiException("No choices returned by the AI provider", HttpStatus.BAD_GATEWAY);
        }
        Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        Object content = message != null ? message.get("content") : null;
        if (content == null) {
            throw new ApiException("Malformed response from the AI provider", HttpStatus.BAD_GATEWAY);
        }
        return content.toString();
    }
}
