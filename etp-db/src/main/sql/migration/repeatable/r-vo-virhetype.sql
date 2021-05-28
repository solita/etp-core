insert into vo_virhetype (id, ordinal, label_fi, label_sv, description_fi, description_sv)
values
(0, 1, 'Test fi', 'TODO fi', 'Test sv', 'TODO sv'),
(1, 1, 'Todistus tehty väärän lain mukaan',
       'Laki rakennuksen energiatodistuksesta annetun lain muuttamisesta (755/2017) mukaan käyttöönottovaiheen energiatodistus tulee päivittää saman lain mukaiseksi kuin lupavaiheen energiatodistus on ollut.',
       'TODO',
       'TODO')
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  description_fi = excluded.description_fi,
  description_sv = excluded.description_sv,
  ordinal = excluded.ordinal;