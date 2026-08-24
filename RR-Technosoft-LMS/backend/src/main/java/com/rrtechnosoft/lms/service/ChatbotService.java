package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.config.ChatbotProperties;
import com.rrtechnosoft.lms.dto.request.SendChatMessageRequest;
import com.rrtechnosoft.lms.dto.response.ChatConversationResponse;
import com.rrtechnosoft.lms.dto.response.ChatMessageResponse;
import com.rrtechnosoft.lms.dto.response.SendChatMessageResponse;
import com.rrtechnosoft.lms.entity.ChatConversation;
import com.rrtechnosoft.lms.entity.ChatMessage;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.ChatRole;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.ChatConversationRepository;
import com.rrtechnosoft.lms.repository.ChatMessageRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.service.chatbot.ChatTurn;
import com.rrtechnosoft.lms.service.chatbot.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final int TITLE_MAX_LENGTH = 60;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final LlmClient llmClient;
    private final ChatbotProperties properties;

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> listConversations(UUID studentId) {
        return conversationRepository.findByStudentIdOrderByUpdatedAtDesc(studentId).stream()
                .map(ChatConversationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(UUID conversationId, UUID studentId) {
        ChatConversation conversation = requireOwnedConversation(conversationId, studentId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public SendChatMessageResponse sendMessage(SendChatMessageRequest request, UUID studentId) {
        ChatConversation conversation = request.conversationId() != null
                ? requireOwnedConversation(request.conversationId(), studentId)
                : startConversation(studentId, request.message());

        List<ChatMessage> priorMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<ChatTurn> history = priorMessages.stream()
                .skip(Math.max(0, priorMessages.size() - properties.getMaxHistoryMessages()))
                .map(m -> new ChatTurn(m.getRole() == ChatRole.USER ? "user" : "assistant", m.getContent()))
                .toList();

        ChatMessage userMessage = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .role(ChatRole.USER)
                .content(request.message())
                .build());

        String replyText = llmClient.generateReply(history, request.message());

        ChatMessage assistantMessage = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .role(ChatRole.ASSISTANT)
                .content(replyText)
                .build());

        conversation.setUpdatedAt(java.time.OffsetDateTime.now());
        conversationRepository.save(conversation);

        return new SendChatMessageResponse(
                conversation.getId(),
                ChatMessageResponse.from(userMessage),
                ChatMessageResponse.from(assistantMessage)
        );
    }

    @Transactional
    public void deleteConversation(UUID conversationId, UUID studentId) {
        ChatConversation conversation = requireOwnedConversation(conversationId, studentId);
        conversationRepository.delete(conversation);
    }

    private ChatConversation startConversation(UUID studentId, String firstMessage) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Student not found"));
        String title = firstMessage.length() > TITLE_MAX_LENGTH
                ? firstMessage.substring(0, TITLE_MAX_LENGTH).trim() + "…"
                : firstMessage.trim();
        return conversationRepository.save(ChatConversation.builder()
                .student(student)
                .title(title.isBlank() ? "New conversation" : title)
                .build());
    }

    private ChatConversation requireOwnedConversation(UUID conversationId, UUID studentId) {
        return conversationRepository.findByIdAndStudentId(conversationId, studentId)
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
    }
}
