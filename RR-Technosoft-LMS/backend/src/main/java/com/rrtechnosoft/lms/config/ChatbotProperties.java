package com.rrtechnosoft.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds app.chatbot.* (see application.yml / CHATBOT_* env vars).
 *
 * Same inert-until-configured pattern as RazorpayProperties / WhatsAppProperties:
 * when `enabled` is false (the default), ChatbotService talks to
 * SimulatedLlmClient instead of OpenAiCompatibleLlmClient, so the whole
 * conversation flow (create conversation, send message, get a reply, see it
 * persisted and rendered) is fully exercisable in local/dev/CI without any
 * real LLM credentials. Flip `enabled: true` and supply `api-key` /
 * `base-url` / `model` to point at a real OpenAI-compatible endpoint
 * (OpenAI itself, Azure OpenAI's compatible route, or a self-hosted
 * vLLM / Ollama server exposing /v1/chat/completions).
 */
@Component
@ConfigurationProperties(prefix = "app.chatbot")
@Getter
@Setter
public class ChatbotProperties {
    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private String systemPrompt =
            "You are the RR Technosoft LMS study assistant. Help students with technical "
            + "questions on AWS, Azure, DevOps, Data Analytics, Linux, Docker, Kubernetes, "
            + "Jenkins, Terraform, Git, Python, SQL, Power BI, Tableau, Java and Spring Boot, "
            + "generate example code/YAML/Terraform/Kubernetes manifests on request, and help "
            + "with interview preparation. Keep answers concise and practical.";
    private int maxHistoryMessages = 20;
}
