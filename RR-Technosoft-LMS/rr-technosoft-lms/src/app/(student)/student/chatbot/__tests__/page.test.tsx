import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import StudentChatbotPage from "../page";
import { chatbotApi } from "@/lib/api/chatbot";

jest.mock("@/lib/api/chatbot", () => ({
  chatbotApi: {
    listConversations: jest.fn(),
    getMessages: jest.fn(),
    sendMessage: jest.fn(),
    deleteConversation: jest.fn(),
  },
}));

jest.mock("sonner", () => ({
  toast: { error: jest.fn(), success: jest.fn() },
}));

const mockedChatbotApi = chatbotApi as jest.Mocked<typeof chatbotApi>;

describe("StudentChatbotPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("shows an empty state when there are no conversations yet", async () => {
    mockedChatbotApi.listConversations.mockResolvedValue([]);

    render(<StudentChatbotPage />);

    expect(await screen.findByText(/No conversations yet/i)).toBeInTheDocument();
    expect(screen.getByText("Ask me anything")).toBeInTheDocument();
  });

  it("lists existing conversations and loads messages when one is selected", async () => {
    mockedChatbotApi.listConversations.mockResolvedValue([
      { id: "conv-1", title: "Docker basics", createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
    ]);
    mockedChatbotApi.getMessages.mockResolvedValue([
      { id: "msg-1", role: "USER", content: "What is Docker?", createdAt: "2026-01-01T00:00:00Z" },
      { id: "msg-2", role: "ASSISTANT", content: "Docker packages apps into containers.", createdAt: "2026-01-01T00:00:01Z" },
    ]);

    const user = userEvent.setup();
    render(<StudentChatbotPage />);

    const conversationEntry = await screen.findByText("Docker basics");
    await user.click(conversationEntry);

    expect(await screen.findByText("What is Docker?")).toBeInTheDocument();
    expect(screen.getByText("Docker packages apps into containers.")).toBeInTheDocument();
    expect(mockedChatbotApi.getMessages).toHaveBeenCalledWith("conv-1");
  });

  it("sends a new message and renders the assistant's reply", async () => {
    mockedChatbotApi.listConversations.mockResolvedValue([]);
    mockedChatbotApi.sendMessage.mockResolvedValue({
      conversationId: "conv-new",
      userMessage: { id: "msg-1", role: "USER", content: "Explain Kubernetes", createdAt: "2026-01-01T00:00:00Z" },
      assistantMessage: {
        id: "msg-2",
        role: "ASSISTANT",
        content: "Kubernetes orchestrates containers across a cluster.",
        createdAt: "2026-01-01T00:00:01Z",
      },
    });

    const user = userEvent.setup();
    render(<StudentChatbotPage />);

    await screen.findByText(/Ask me anything/i);

    const textbox = screen.getByPlaceholderText("Ask a question…");
    await user.type(textbox, "Explain Kubernetes");
    await user.click(screen.getByRole("button", { name: "Send message" }));

    await waitFor(() =>
      expect(mockedChatbotApi.sendMessage).toHaveBeenCalledWith({ conversationId: undefined, message: "Explain Kubernetes" })
    );
    expect(await screen.findByText("Kubernetes orchestrates containers across a cluster.")).toBeInTheDocument();
  });

  it("does not send an empty message", async () => {
    mockedChatbotApi.listConversations.mockResolvedValue([]);

    const user = userEvent.setup();
    render(<StudentChatbotPage />);

    await screen.findByText(/Ask me anything/i);
    await user.click(screen.getByRole("button", { name: "Send message" }));

    expect(mockedChatbotApi.sendMessage).not.toHaveBeenCalled();
  });

  it("starting a new chat clears the active conversation", async () => {
    mockedChatbotApi.listConversations.mockResolvedValue([
      { id: "conv-1", title: "Docker basics", createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
    ]);
    mockedChatbotApi.getMessages.mockResolvedValue([
      { id: "msg-1", role: "USER", content: "What is Docker?", createdAt: "2026-01-01T00:00:00Z" },
    ]);

    const user = userEvent.setup();
    render(<StudentChatbotPage />);

    await user.click(await screen.findByText("Docker basics"));
    await screen.findByText("What is Docker?");

    await user.click(screen.getByRole("button", { name: /New chat/i }));

    await waitFor(() => expect(screen.queryByText("What is Docker?")).not.toBeInTheDocument());
    expect(screen.getByText(/Ask me anything/i)).toBeInTheDocument();
  });
});
