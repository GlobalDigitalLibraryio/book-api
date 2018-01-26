ALTER TABLE translation ADD COLUMN translated_from TEXT, ADD COLUMN publishing_status TEXT NOT NULL DEFAULT 'PUBLISHED';
