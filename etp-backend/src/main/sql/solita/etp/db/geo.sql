
-- name: select-countries
select id, label_fi "label-fi", label_sv "label-sv", true "valid" from country
where label_fi is not null and label_sv is not null