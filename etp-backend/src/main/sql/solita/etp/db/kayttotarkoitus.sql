-- name: select-kayttotarkoitusluokat-by-versio
select id, label_fi "label-fi", label_sv "label-sv", deleted
from kayttotarkoitusluokka where versio = :versio
order by ordinal asc

-- name: select-alakayttotarkoitusluokat-by-versio
select id, kayttotarkoitusluokka_id "kayttotarkoitusluokka-id", label_fi "label-fi", label_sv "label-sv", deleted
from alakayttotarkoitusluokka where versio = :versio
order by ordinal asc

-- name: select-kayttotarkoitusluokka-id-by-versio-and-alakayttotarkoitusluokka-id
select k.id
from kayttotarkoitusluokka k
left join alakayttotarkoitusluokka a ON k.id = a.kayttotarkoitusluokka_id
where k.versio = :versio and a.versio = :versio and a.id = :id and k.deleted = false
order by k.ordinal asc
