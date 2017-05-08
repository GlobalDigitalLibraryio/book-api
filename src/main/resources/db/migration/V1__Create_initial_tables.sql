CREATE TABLE readingmaterial (
  id BIGSERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  document JSONB
);

CREATE TABLE readingmaterialinlanguage (
  id BIGSERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  reading_material_id BIGSERIAL REFERENCES readingmaterial(id) ON DELETE CASCADE,
  document JSONB
);
