alter table translation add check (external_id <> '');
alter table translation add constraint UNI_TRANSLATION_EXTERNAL_ID unique (external_id);
