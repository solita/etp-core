alter table laatija drop column api_key_hash;
alter table audit.laatija drop column api_key_hash;
call audit.create_audit_procedure('laatija'::name);
