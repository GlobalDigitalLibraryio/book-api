ALTER TABLE translation ADD COLUMN page_orientation text NOT NULL DEFAULT 'PORTRAIT';
CREATE OR REPLACE VIEW translation_not_flagged AS select * from translation where publishing_status <> 'FLAGGED';
update translation set page_orientation = 'LANDSCAPE' where book_id in (select id from book where source = 'storyweaver');