CREATE TABLE featured_content (
  id          BIGSERIAL PRIMARY KEY,
  revision    INTEGER NOT NULL DEFAULT 1,
  language    TEXT    NOT NULL,
  title       TEXT    NOT NULL,
  description TEXT    NOT NULL,
  link        TEXT    NOT NULL,
  image_url   TEXT    NOT NULL
);
