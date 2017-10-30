CREATE TABLE feed (
  id BIGSERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  url TEXT NOT NULL,
  uuid TEXT NOT NULL,
  title_key TEXT NOT NULL,
  description_key TEXT
);
