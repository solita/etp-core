-- name: select-lammitysmuodot
SELECT id, label_fi "label-fi", label_sv "label-sv", valid
FROM lammitysmuoto
ORDER BY ordinal ASC

-- name: select-lammonjaot
SELECT id, label_fi "label-fi", label_sv "label-sv", valid
FROM lammonjako
ORDER BY ordinal ASC
