package com.rrtechnosoft.lms.dto.response;

import java.util.UUID;

/** Returned by POST /chatbot/messages — carries the (possibly newly-created) conversation id
 *  back to the frontend along with both the saved user message and the generated reply. */
public record SendChatMessageResponse(
        UUID conversationId,
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage
) {}
