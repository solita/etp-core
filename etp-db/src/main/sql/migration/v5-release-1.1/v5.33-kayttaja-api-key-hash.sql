alter table kayttaja add column api_key_hash text;

update kayttaja
set api_key_hash = laatija.api_key_hash
from laatija
where kayttaja.id = laatija.id;

alter table laatija drop column api_key_hash;

alter table audit.kayttaja add column api_key_hash text;
alter table audit.laatija drop column api_key_hash;

call audit.create_audit_procedure('kayttaja'::name);
call audit.create_audit_procedure('laatija'::name);
