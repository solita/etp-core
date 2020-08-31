
insert into validation_numeric_column
  (column_name, versio, warning$min, warning$max, error$min, error$max)
values
('lt$rakennusvaippa$ulkoseinat$U', 2018, 0.08, 0.81, 0.05, 2),
('lt$rakennusvaippa$ylapohja$U',   2018, 0.05, 0.47, 0.03, 2),
('lt$rakennusvaippa$alapohja$U',   2018, 0.05, 0.60, 0.03, 4),
('lt$rakennusvaippa$ikkunat$U',    2018, 0.05, 3.10, 0.04, 6.5),
('lt$rakennusvaippa$ulkoovet$U',   2018, 0.08, 0.81, 0.05, 2)
on conflict (column_name, versio) do update
  set
    warning$min = excluded.warning$min,
    warning$max = excluded.warning$max,
    error$min = excluded.error$min,
    error$max = excluded.error$max;