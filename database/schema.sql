CREATE SCHEMA IF NOT EXISTS groupapi AUTHORIZATION sys;

CREATE TABLE IF NOT EXISTS groupapi.app_users (
    id varchar(255) PRIMARY KEY,
    name varchar(255) NOT NULL,
    username varchar(255) NOT NULL UNIQUE,
    email varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    avatar_url varchar(1000),
    is_online boolean NOT NULL DEFAULT false,
    last_active_at timestamptz
);

CREATE TABLE IF NOT EXISTS groupapi.conversations (
    id varchar(255) PRIMARY KEY,
    type varchar(20) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name varchar(255),
    avatar_url varchar(1000),
    created_by_user_id varchar(255) REFERENCES groupapi.app_users(id),
    last_message_id varchar(255),
    last_message_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    deleted_at timestamptz
);

CREATE TABLE IF NOT EXISTS groupapi.messages (
    id varchar(255) PRIMARY KEY,
    conversation_id varchar(255) NOT NULL REFERENCES groupapi.conversations(id),
    sender_id varchar(255) NOT NULL REFERENCES groupapi.app_users(id),
    body varchar(4000) NOT NULL,
    message_type varchar(20) NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'SYSTEM', 'ATTACHMENT')),
    reply_to_message_id varchar(255) REFERENCES groupapi.messages(id),
    forwarded_from_message_id varchar(255) REFERENCES groupapi.messages(id),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    deleted_at timestamptz
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_conversations_last_message'
          AND connamespace = 'groupapi'::regnamespace
    ) THEN
        ALTER TABLE groupapi.conversations
            ADD CONSTRAINT fk_conversations_last_message
            FOREIGN KEY (last_message_id)
            REFERENCES groupapi.messages(id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS groupapi.conversation_members (
    id varchar(255) PRIMARY KEY,
    conversation_id varchar(255) NOT NULL REFERENCES groupapi.conversations(id),
    user_id varchar(255) NOT NULL REFERENCES groupapi.app_users(id),
    role varchar(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('MEMBER', 'ADMIN')),
    last_read_message_id varchar(255) REFERENCES groupapi.messages(id),
    last_read_at timestamptz,
    muted_until timestamptz,
    nickname varchar(255),
    joined_at timestamptz NOT NULL,
    deleted_at timestamptz,
    CONSTRAINT uq_conversation_members UNIQUE (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS groupapi.message_receipts (
    id varchar(255) PRIMARY KEY,
    message_id varchar(255) NOT NULL REFERENCES groupapi.messages(id),
    user_id varchar(255) NOT NULL REFERENCES groupapi.app_users(id),
    status varchar(20) NOT NULL CHECK (status IN ('SENT', 'DELIVERED', 'READ')),
    delivered_at timestamptz,
    read_at timestamptz,
    CONSTRAINT uq_message_receipts UNIQUE (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_conversation_members_user_id
    ON groupapi.conversation_members(user_id);

CREATE INDEX IF NOT EXISTS idx_conversation_members_conversation_id
    ON groupapi.conversation_members(conversation_id);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_created_at
    ON groupapi.messages(conversation_id, created_at);

CREATE INDEX IF NOT EXISTS idx_messages_sender_id
    ON groupapi.messages(sender_id);

CREATE INDEX IF NOT EXISTS idx_message_receipts_message_id
    ON groupapi.message_receipts(message_id);

CREATE INDEX IF NOT EXISTS idx_message_receipts_user_id
    ON groupapi.message_receipts(user_id);
