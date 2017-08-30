CREATE TABLE book (
  id BIGSERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  external_id TEXT,
  document JSONB
);

CREATE TABLE bookinlanguage (
  id BIGSERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  book_id BIGSERIAL REFERENCES book(id) ON DELETE CASCADE,
  external_id TEXT,
  document JSONB
);
