ALTER TABLE translation ADD COLUMN book_type text NOT NULL DEFAULT 'BOOK';
CREATE OR REPLACE VIEW translation_not_flagged AS select * from translation where publishing_status <> 'FLAGGED';