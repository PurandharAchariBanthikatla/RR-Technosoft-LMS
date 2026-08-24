import { apiClient } from "./client";
import { API_ROUTES } from "@/lib/constants";
import { ChatConversation, ChatMessage, SendChatMessageResult } from "@/types";

export const chatbotApi = {
  listConversations: () =>
    apiClient.get<ChatConversation[]>(API_ROUTES.chatbotConversations).then((r) => r.data),

  getMessages: (conversationId: string) =>
    apiClient.get<ChatMessage[]>(API_ROUTES.chatbotConversationMessages(conversationId)).then((r) => r.data),

  sendMessage: (payload: { conversationId?: string | null; message: string }) =>
    apiClient.post<SendChatMessageResult>(API_ROUTES.chatbotMessages, payload).then((r) => r.data),

  deleteConversation: (conversationId: string) =>
    apiClient.delete(API_ROUTES.chatbotConversationById(conversationId)).then((r) => r.data),
};
