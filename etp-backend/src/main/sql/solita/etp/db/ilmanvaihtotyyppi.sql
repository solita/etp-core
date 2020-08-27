-- name: select-ilmanvaihtotyypit
SELECT id, label_fi "label-fi", label_sv "label-sv", valid
FROM ilmanvaihtotyyppi
ORDER BY ordinal ASC
