
alter table kayttaja add column verifytime timestamp with time zone;

/*
alter table audit.kayttaja
  add column verifytime timestamp with time zone;
call audit.create_audit_procedure('kayttaja'::name);
drop trigger kayttaja_update_trigger on kayttaja;
call audit.create_audit_update_trigger(
  'kayttaja'::name, 'kayttaja'::name,
  audit.update_condition('kayttaja'::name));
*/