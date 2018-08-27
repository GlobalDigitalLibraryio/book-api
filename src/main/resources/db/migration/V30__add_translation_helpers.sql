ALTER TABLE translation ADD COLUMN in_transport boolean NOT NULL DEFAULT false;
ALTER TABLE translation ADD COLUMN translation_status text;
CREATE OR REPLACE VIEW translation_not_flagged AS select * from translation where publishing_status <> 'FLAGGED';