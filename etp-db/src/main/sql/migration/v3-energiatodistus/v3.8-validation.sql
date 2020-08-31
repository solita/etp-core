
create table validation_numeric_column (
  column_name text,
  versio int,
  warning$min numeric,
  warning$max numeric,
  error$min numeric,
  error$max numeric,

  primary key (column_name, versio)
)