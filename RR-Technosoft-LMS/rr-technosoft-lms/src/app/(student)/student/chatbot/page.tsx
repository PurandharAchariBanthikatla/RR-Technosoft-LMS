"use client";

import { useEffect, useRef, useState } from "react";
import { Bot, Loader2, MessageSquarePlus, Send, Trash2, User as UserIcon } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state";
import { useFetch } from "@/hooks/use-fetch";
import { chatbotApi } from "@/lib/api/chatbot";
import { extractErrorMessage } from "@/lib/api/client";
import { cn } from "@/lib/utils";
import { ChatMessage } from "@/types";
import { toast } from "sonner";

export default function StudentChatbotPage() {
  const { data: conversations, isLoading: loadingConversations, error: conversationsError, refetch: refetchConversations } =
    useFetch(() => chatbotApi.listConversations(), []);

  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  // Set right before we assign a freshly-created conversation id after a
  // send, so the id-change effect below doesn't immediately re-fetch and
  // clobber the messages we already have in state (and, in tests, doesn't
  // call getMessages for a conversation the test never mocked a response for).
  const skipNextMessageFetchRef = useRef(false);

  useEffect(() => {
    if (!activeConversationId) {
      setMessages([]);
      return;
    }
    if (skipNextMessageFetchRef.current) {
      skipNextMessageFetchRef.current = false;
      return;
    }
    let cancelled = false;
    setLoadingMessages(true);
    chatbotApi
      .getMessages(activeConversationId)
      .then((data) => {
        if (!cancelled) setMessages(data);
      })
      .catch((err) => toast.error(extractErrorMessage(err)))
      .finally(() => {
        if (!cancelled) setLoadingMessages(false);
      });
    return () => {
      cancelled = true;
    };
  }, [activeConversationId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, sending]);

  async function handleSend() {
    const trimmed = input.trim();
    if (!trimmed || sending) return;

    setSending(true);
    setInput("");
    const optimisticUserMessage: ChatMessage = {
      id: `pending-${Date.now()}`,
      role: "USER",
      content: trimmed,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, optimisticUserMessage]);

    try {
      const result = await chatbotApi.sendMessage({
        conversationId: activeConversationId ?? undefined,
        message: trimmed,
      });
      setMessages((prev) => [
        ...prev.filter((m) => m.id !== optimisticUserMessage.id),
        result.userMessage,
        result.assistantMessage,
      ]);
      if (!activeConversationId) {
        skipNextMessageFetchRef.current = true;
        setActiveConversationId(result.conversationId);
      }
      refetchConversations();
    } catch (err) {
      toast.error(extractErrorMessage(err));
      setMessages((prev) => prev.filter((m) => m.id !== optimisticUserMessage.id));
      setInput(trimmed);
    } finally {
      setSending(false);
    }
  }

  async function handleDelete(conversationId: string) {
    try {
      await chatbotApi.deleteConversation(conversationId);
      if (activeConversationId === conversationId) {
        setActiveConversationId(null);
      }
      refetchConversations();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  return (
    <div>
      <PageHeader
        title="AI Assistant"
        description="Ask about AWS, DevOps, Docker, Kubernetes, Terraform, Git, Python, SQL, Java/Spring Boot, or interview prep."
        actions={
          <Button variant="outline" size="sm" className="gap-1.5" onClick={() => setActiveConversationId(null)}>
            <MessageSquarePlus className="h-4 w-4" /> New chat
          </Button>
        }
      />

      <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
        {/* Conversation list */}
        <Card className="h-fit">
          <CardContent className="p-2">
            {loadingConversations ? (
              <div className="space-y-2 p-2">
                {Array.from({ length: 4 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : conversationsError ? (
              <p className="p-3 text-sm text-muted-foreground">Couldn&apos;t load conversations.</p>
            ) : !conversations || conversations.length === 0 ? (
              <p className="p-3 text-sm text-muted-foreground">No conversations yet — send a message to start one.</p>
            ) : (
              <div className="space-y-1">
                {conversations.map((c) => (
                  <div
                    key={c.id}
                    className={cn(
                      "group flex items-center justify-between gap-1 rounded-md px-2 py-2 text-sm cursor-pointer hover:bg-muted",
                      activeConversationId === c.id && "bg-muted font-medium"
                    )}
                    onClick={() => setActiveConversationId(c.id)}
                  >
                    <span className="truncate">{c.title}</span>
                    <button
                      aria-label="Delete conversation"
                      className="shrink-0 opacity-0 group-hover:opacity-100"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(c.id);
                      }}
                    >
                      <Trash2 className="h-3.5 w-3.5 text-muted-foreground hover:text-destructive" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Message thread */}
        <Card className="flex h-[65vh] flex-col">
          <CardContent className="flex flex-1 flex-col overflow-hidden p-4">
            <div className="flex-1 space-y-4 overflow-y-auto pr-1">
              {loadingMessages ? (
                <div className="space-y-3">
                  {Array.from({ length: 3 }).map((_, i) => (
                    <Skeleton key={i} className="h-12 w-2/3" />
                  ))}
                </div>
              ) : messages.length === 0 ? (
                <EmptyState
                  icon={Bot}
                  title="Ask me anything"
                  description="Try “explain Kubernetes deployments” or “help me prep for a DevOps interview”."
                />
              ) : (
                messages.map((m) => (
                  <div key={m.id} className={cn("flex gap-2", m.role === "USER" && "flex-row-reverse")}>
                    <div
                      className={cn(
                        "flex h-7 w-7 shrink-0 items-center justify-center rounded-full",
                        m.role === "USER" ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
                      )}
                    >
                      {m.role === "USER" ? <UserIcon className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
                    </div>
                    <div
                      className={cn(
                        "max-w-[75%] whitespace-pre-wrap rounded-lg px-3 py-2 text-sm",
                        m.role === "USER" ? "bg-primary text-primary-foreground" : "bg-muted"
                      )}
                    >
                      {m.content}
                    </div>
                  </div>
                ))
              )}
              {sending && (
                <div className="flex gap-2">
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground">
                    <Bot className="h-4 w-4" />
                  </div>
                  <div className="flex items-center gap-1.5 rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">
                    <Loader2 className="h-3.5 w-3.5 animate-spin" /> Thinking…
                  </div>
                </div>
              )}
              <div ref={bottomRef} />
            </div>

            <div className="mt-3 flex items-end gap-2 border-t pt-3">
              <Textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    handleSend();
                  }
                }}
                placeholder="Ask a question…"
                rows={2}
                className="resize-none"
                disabled={sending}
              />
              <Button size="icon" onClick={handleSend} disabled={sending || !input.trim()} aria-label="Send message">
                {sending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
