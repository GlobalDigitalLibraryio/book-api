ALTER TABLE feed ADD COLUMN updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();
