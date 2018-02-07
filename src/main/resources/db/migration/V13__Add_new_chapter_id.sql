ALTER TABLE in_translation_file RENAME COLUMN original_id TO original_chapter_id;
ALTER TABLE in_translation_file ADD COLUMN new_chapter_id bigint;