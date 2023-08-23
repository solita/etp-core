-- name: find-attachment-name-by-hallinto-oikeus-id
select attachment_name
from hallinto_oikeus
where id = :hallinto-oikeus-id;
