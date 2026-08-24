-- Align chat_messages schema with ChatMessage entity.

ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS role VARCHAR(20);

UPDATE chat_messages
SET role = CASE
    WHEN sender = 'BOT' THEN 'ASSISTANT'
    ELSE sender
END
WHERE role IS NULL;

ALTER TABLE chat_messages
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_chat_messages_role
    CHECK (role IN ('USER', 'ASSISTANT'));
