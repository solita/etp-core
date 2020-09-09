
insert into validation_numeric_column
  (versio, column_name, warning$min, warning$max, error$min, error$max)
values
(2018, 'lt$rakennusvaippa$ulkoseinat$U', 0.08, 0.81, 0.05, 2),
(2018, 'lt$rakennusvaippa$ylapohja$U',   0.05, 0.47, 0.03, 2),
(2018, 'lt$rakennusvaippa$alapohja$U',   0.05, 0.60, 0.03, 4),
(2018, 'lt$rakennusvaippa$ikkunat$U',    0.05, 3.10, 0.04, 6.5),
(2018, 'lt$rakennusvaippa$ulkoovet$U',   0.08, 0.81, 0.05, 2)
on conflict (column_name, versio) do update
  set
    warning$min = excluded.warning$min,
    warning$max = excluded.warning$max,
    error$min = excluded.error$min,
    error$max = excluded.error$max;

insert into validation_required_column (versio, column_name)
values
(2018, 'pt$nimi'),
(2018, 'lt$rakennusvaippa$ulkoseinat$U'),
(2018, 'lt$rakennusvaippa$ylapohja$U'),
(2018, 'lt$rakennusvaippa$alapohja$U'),
(2018, 'lt$rakennusvaippa$ikkunat$U'),
(2018, 'lt$rakennusvaippa$ulkoovet$U')
on conflict (column_name, versio) do nothing