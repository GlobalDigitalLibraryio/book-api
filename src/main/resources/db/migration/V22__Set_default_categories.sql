alter table category add constraint UNI_CATEGORY_NAME unique (name);
insert into category (revision, name) values (1, 'library_books') on conflict do nothing;
update translation set category_ids = array(select id from category where name = 'library_books');
