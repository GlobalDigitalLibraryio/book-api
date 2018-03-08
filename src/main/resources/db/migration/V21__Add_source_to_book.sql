ALTER TABLE book ADD COLUMN source text NOT NULL DEFAULT 'unknown';
update book set source = 'african_storybook' where publisher_id = (select id from publisher where name = 'African Storybook Initiative');
update book set source = 'ew' where publisher_id = (select id from publisher where name = 'ew');
update book set source = 'storyweaver' where publisher_id = (select id from publisher where name = 'Pratham Books');
update book set source = 'storyweaver' where publisher_id = (select id from publisher where name = 'The Asia Foundation');
update book set source = 'storyweaver' where publisher_id = (select id from publisher where name = 'StoryWeaver Community');
update book set source = 'storyweaver' where publisher_id = (select id from publisher where name = 'StoryWeaver');
