package com.rrtechnosoft.lms.service.chatbot;

import java.util.List;

/**
 * The one integration point ChatbotService depends on. Exactly one
 * implementation is active at a time, chosen by app.chatbot.enabled:
 *   - SimulatedLlmClient (default) — deterministic, no network call.
 *   - OpenAiCompatibleLlmClient — real HTTP call, active once enabled
 *     and an api-key is configured.
 * Swapping providers (a different vendor, a different auth scheme) means
 * adding a new implementation of this interface and flipping the
 * @ConditionalOnProperty on the two existing ones — ChatbotService,
 * the controller, and everything on the frontend stay unchanged.
 */
public interface LlmClient {

    /**
     * @param history prior turns in the conversation, oldest first, already
     *                capped to app.chatbot.max-history-messages by the caller
     * @param newUserMessage the message the student just sent
     * @return the assistant's reply text
     */
    String generateReply(List<ChatTurn> history, String newUserMessage);
}
