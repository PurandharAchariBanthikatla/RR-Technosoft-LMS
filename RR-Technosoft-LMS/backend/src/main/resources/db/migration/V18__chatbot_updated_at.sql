-- Add updated_at required by ChatConversation entity.

ALTER TABLE chat_conversations
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
