CREATE TABLE category (
  id SERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  name TEXT NOT NULL
);

CREATE TABLE person (
  id SERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  name TEXT NOT NULL
);

CREATE TABLE license (
  id SERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  name TEXT NOT NULL,
  description TEXT,
  url TEXT
);

insert into license (name, description, url) values ('cc by 4.0', 'Attribution 4.0 International (CC BY 4.0)', 'https://creativecommons.org/licenses/by/4.0/');
insert into license (name, description, url) values ('cc by-sa 4.0', 'Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)', 'https://creativecommons.org/licenses/by-sa/4.0/');
insert into license (name, description, url) values ('cc by-nd 4.0', 'Attribution-NoDerivatives 4.0 International (CC BY-ND 4.0)', 'https://creativecommons.org/licenses/by-nd/4.0/');
insert into license (name, description, url) values ('publicdomain', 'Public Domain Mark', 'https://creativecommons.org/share-your-work/public-domain/pdm');

CREATE TABLE publisher (
  id SERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  name TEXT NOT NULL
);

DROP TABLE bookinlanguage;

ALTER TABLE BOOK ADD COLUMN publisher_id INTEGER REFERENCES publisher(id);
ALTER TABLE BOOK ADD COLUMN license_id INTEGER REFERENCES license(id);
ALTER TABLE BOOK DROP COLUMN document;
ALTER TABLE BOOK DROP COLUMN external_id;

CREATE TABLE educational_alignment(
  id SERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  alignment_type TEXT,
  educational_framework TEXT,
  target_description TEXT,
  target_name TEXT,
  target_url TEXT
);

CREATE TABLE translation (
  id BIGSERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  book_id BIGINT REFERENCES book(id),
  external_id TEXT,
  uuid TEXT NOT NULL,
  title TEXT NOT NULL,
  about TEXT NOT NULL,
  num_pages INTEGER,
  language TEXT NOT NULL,
  date_published DATE,
  date_created DATE,
  category_ids BIGINT ARRAY,
  coverphoto BIGINT,
  tags text ARRAY,
  is_based_on_url TEXT,
  educational_use TEXT,
  educational_role TEXT,
  ea_id INTEGER REFERENCES educational_alignment(id),
  time_required TEXT,
  typical_age_range TEXT,
  reading_level TEXT,
  interactivity_type TEXT,
  learning_resource_type TEXT,
  accessibility_api TEXT,
  accessibility_control TEXT,
  accessibility_feature TEXT,
  accessibility_hazard TEXT
);

CREATE TABLE chapter (
  id BIGSERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  translation_id BIGINT REFERENCES translation(id) NOT NULL,
  seq_no INTEGER NOT NULL,
  title text,
  content text NOT NULL
);

CREATE TABLE contributor (
  id BIGSERIAL PRIMARY KEY,
  revision INTEGER NOT NULL DEFAULT 1,
  person_id INTEGER REFERENCES person(id) NOT NULL,
  translation_id BIGINT REFERENCES translation(id) NOT NULL,
  type TEXT NOT NULL
);

