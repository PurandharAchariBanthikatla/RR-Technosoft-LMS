package com.rrtechnosoft.lms.service.chatbot;

/** One turn of conversation history handed to an LlmClient. role is "user" or "assistant". */
public record ChatTurn(String role, String content) {}
