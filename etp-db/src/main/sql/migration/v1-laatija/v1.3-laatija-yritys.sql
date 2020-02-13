create table laatija_yritys (
  laatija_id int references laatija (id),
  yritys_id int references yritys (id)
);
