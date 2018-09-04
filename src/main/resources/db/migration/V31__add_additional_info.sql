ALTER TABLE translation ADD COLUMN additional_information text;
CREATE OR REPLACE VIEW translation_not_flagged AS select * from translation where publishing_status <> 'FLAGGED';