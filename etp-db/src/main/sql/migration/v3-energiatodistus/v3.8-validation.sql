
create table validation_numeric_column (
  column_name text,
  versio int,
  warning$min numeric,
  warning$max numeric,
  error$min numeric,
  error$max numeric,
  valid boolean default true,
  primary key (column_name, versio)
);

create table validation_required_column (
  column_name text,
  versio int,
  valid boolean default true,

  primary key (column_name, versio)
);