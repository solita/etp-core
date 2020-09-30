
-- name: select-countries
select id, label_fi, label_sv, valid from country
where label_fi is not null and label_sv is not null

-- name: select-toiminta-alueet
select id, label_fi, label_sv, valid from toimintaalue

-- name: select-postinumerot
select id, label_fi, label_sv, valid from postinumero