-- Create calls table
CREATE TABLE calls (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    initiator_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    duration_seconds INTEGER,
    CONSTRAINT fk_call_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_call_initiator FOREIGN KEY (initiator_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_call_type CHECK (type IN ('AUDIO', 'VIDEO')),
    CONSTRAINT chk_call_status CHECK (status IN ('INITIATED', 'RINGING', 'ONGOING', 'ENDED', 'MISSED', 'REJECTED', 'FAILED'))
);

-- Create call_participants table
CREATE TABLE call_participants (
    call_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP,
    left_at TIMESTAMP,
    PRIMARY KEY (call_id, user_id),
    CONSTRAINT fk_call_participant_call FOREIGN KEY (call_id) REFERENCES calls(id) ON DELETE CASCADE,
    CONSTRAINT fk_call_participant_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_calls_conversation_id ON calls(conversation_id);
CREATE INDEX idx_calls_initiator_id ON calls(initiator_id);
CREATE INDEX idx_calls_status ON calls(status);
CREATE INDEX idx_calls_started_at ON calls(started_at);
CREATE INDEX idx_call_participants_user_id ON call_participants(user_id);

