ALTER TABLE translation ADD COLUMN officially_approved boolean not null DEFAULT false;
CREATE OR REPLACE VIEW translation_not_flagged AS select * from translation where publishing_status <> 'FLAGGED';