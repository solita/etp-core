-- name: select-kayttotarkoitusluokat-by-versio
select id, label_fi "label-fi", label_sv "label-sv", valid
from kayttotarkoitusluokka where versio = :versio
order by ordinal asc

-- name: select-alakayttotarkoitusluokat-by-versio
select id, kayttotarkoitusluokka_id "kayttotarkoitusluokka-id", label_fi "label-fi", label_sv "label-sv", valid
from alakayttotarkoitusluokka where versio = :versio
order by ordinal asc

-- name: select-kayttotarkoitusluokka-by-versio-and-alakayttotarkoitusluokka-id
select k.id, k.label_fi "label-fi", k.label_sv "label-sv"
from kayttotarkoitusluokka k
left join alakayttotarkoitusluokka a ON k.id = a.kayttotarkoitusluokka_id
where k.versio = :versio and a.versio = :versio and a.id = :id and k.valid = false
order by k.ordinal asc
