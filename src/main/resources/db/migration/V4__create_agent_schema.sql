-- Drop examples table and its index
DROP TABLE IF EXISTS examples;

-- Create models table
CREATE TABLE models (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    api_format VARCHAR(50) NOT NULL,
    api_key VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    context_limit INT,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_models_owner ON models(owner_id);

-- Create sessions table
CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    system_prompt TEXT,
    model_id UUID NOT NULL REFERENCES models(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    enabled_tools TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_owner ON sessions(owner_id);
CREATE INDEX idx_sessions_model ON sessions(model_id);

-- Create chat_messages table
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    tool_name VARCHAR(255),
    tool_call_id VARCHAR(255),
    step_number INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_session ON chat_messages(session_id);
