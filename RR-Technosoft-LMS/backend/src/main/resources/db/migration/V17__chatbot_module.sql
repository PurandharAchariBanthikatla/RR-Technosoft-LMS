-- AI Chatbot module.
--
-- Each student can have several conversations; each conversation holds an
-- ordered thread of user/assistant messages. `role` is stored as plain
-- VARCHAR rather than a native Postgres ENUM (unlike user_role/account_status
-- in V1) since it's only ever read/written by ChatMessage and never needs a
-- DB-level CHECK beyond the two values enforced in code — one less type to
-- manage in a module that's otherwise fully self-contained in these two
-- tables. See ChatbotService / SimulatedLlmClient / OpenAiCompatibleLlmClient
-- for how a reply is generated: this migration only adds the storage.

CREATE TABLE chat_conversations (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL DEFAULT 'New conversation',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_conversations_student ON chat_conversations (student_id, updated_at DESC);

CREATE TABLE chat_messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES chat_conversations (id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_conversation ON chat_messages (conversation_id, created_at);
