package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.SendChatMessageRequest;
import com.rrtechnosoft.lms.dto.response.ChatConversationResponse;
import com.rrtechnosoft.lms.dto.response.ChatMessageResponse;
import com.rrtechnosoft.lms.dto.response.SendChatMessageResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Backs the student "AI Assistant" screen
 * (src/app/(student)/student/chatbot/page.tsx -> lib/api/chatbot.ts -> /chatbot).
 * URL-level access is already restricted to STUDENT by SecurityConfig's
 * "/chatbot/**" matcher, so no per-method @PreAuthorize is needed here.
 */
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatConversationResponse>> listConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatbotService.listConversations(principal.getId()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(chatbotService.getMessages(id, principal.getId()));
    }

    @PostMapping("/messages")
    public ResponseEntity<SendChatMessageResponse> sendMessage(
            @Valid @RequestBody SendChatMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatbotService.sendMessage(request, principal.getId()));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        chatbotService.deleteConversation(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
