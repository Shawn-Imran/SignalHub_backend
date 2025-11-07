-- Create broadcasts table
CREATE TABLE broadcasts (
    id UUID PRIMARY KEY,
    creator_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    CONSTRAINT fk_broadcast_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_broadcast_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'SENT', 'FAILED'))
);

-- Create broadcast_recipients table
CREATE TABLE broadcast_recipients (
    broadcast_id UUID NOT NULL,
    user_id UUID NOT NULL,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    PRIMARY KEY (broadcast_id, user_id),
    CONSTRAINT fk_broadcast_recipient_broadcast FOREIGN KEY (broadcast_id) REFERENCES broadcasts(id) ON DELETE CASCADE,
    CONSTRAINT fk_broadcast_recipient_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create broadcast_messages table
CREATE TABLE broadcast_messages (
    id UUID PRIMARY KEY,
    broadcast_id UUID NOT NULL,
    content TEXT NOT NULL,
    media_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_broadcast_message_broadcast FOREIGN KEY (broadcast_id) REFERENCES broadcasts(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_broadcasts_creator_id ON broadcasts(creator_id);
CREATE INDEX idx_broadcasts_status ON broadcasts(status);
CREATE INDEX idx_broadcasts_scheduled_at ON broadcasts(scheduled_at);
CREATE INDEX idx_broadcast_recipients_user_id ON broadcast_recipients(user_id);
CREATE INDEX idx_broadcast_messages_broadcast_id ON broadcast_messages(broadcast_id);

