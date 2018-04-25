alter table translation alter column external_id set not null;
alter table translation add check (external_id <> '');
alter table translation add constraint UNI_TRANSLATION_EXTERNAL_ID unique (external_id);
