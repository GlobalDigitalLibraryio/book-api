CREATE TABLE in_translation (
  id serial primary key,
  revision INTEGER NOT NULL DEFAULT 1,
  user_ids text array,
  original_translation_id bigint not null,
  new_translation_id bigint,
  from_language text not null,
  to_language text not null,
  crowdin_to_language text not null,
  crowdin_project_id text not null,
  unique(original_translation_id, to_language, crowdin_project_id)
);

CREATE TABLE in_translation_file (
  id serial primary key,
  revision INTEGER NOT NULL DEFAULT 1,
  in_translation_id bigint references in_translation(id),
  file_type text not null,
  original_id bigint,
  filename text,
  crowdin_file_id text,
  translation_status text,
  etag text
);
