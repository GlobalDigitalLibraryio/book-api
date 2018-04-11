CREATE VIEW translation_not_flagged AS select * from translation where publishing_status <> 'FLAGGED';
