alter table kayttaja add column api_key_hash text;

update kayttaja
set api_key_hash = laatija.api_key_hash
from laatija
where kayttaja.id = laatija.id;

alter table audit.kayttaja add column api_key_hash text;

call audit.create_audit_procedure('kayttaja'::name);
