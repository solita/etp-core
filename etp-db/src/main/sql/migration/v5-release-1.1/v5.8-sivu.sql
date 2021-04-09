create table sivu (
  id int generated by default as identity primary key,
  parent_id int references sivu (id),
  ordinal int,
  published boolean not null default false,

  title text not null,
  -- html or markdown
  body text not null
);
