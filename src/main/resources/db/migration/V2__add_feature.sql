ALTER TABLE examples ADD COLUMN priority INT NOT NULL DEFAULT 0;

CREATE INDEX idx_examples_priority ON examples (priority);
