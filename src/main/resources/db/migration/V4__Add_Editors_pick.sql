CREATE TABLE editors_pick (
  id BIGSERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  language TEXT NOT NULL,
  translation_ids BIGINT ARRAY NOT NULL DEFAULT array[]::bigint[]
);
