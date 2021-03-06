call create_classification('rooli'::name);

create table kayttaja (
  id int generated by default as identity primary key,
  etunimi text not null,
  sukunimi text not null,
  email text not null unique,
  puhelin text not null,
  passivoitu boolean default false not null,
  rooli_id int default 0 not null references rooli (id),
  login timestamp with time zone,
  ensitallennus boolean default false,
  henkilotunnus text unique,
  cognito_id text,
  virtu$localid text,
  virtu$organisaatio text,
  constraint kayttaja_virtu_key unique (virtu$localid, virtu$organisaatio)
);

call audit.create_audit_table('kayttaja'::name);
alter table audit.kayttaja drop column login;

call audit.create_audit_procedure('kayttaja'::name);
call audit.create_audit_insert_trigger('kayttaja'::name, 'kayttaja'::name);
call audit.create_audit_update_trigger('kayttaja'::name, 'kayttaja'::name,
  audit.update_condition('kayttaja'::name));

/* activate auditing to geo tables */
call audit.activate('country'::name);
call audit.activate('toimintaalue'::name);
call audit.activate('kunta'::name);
call audit.activate('postinumero'::name);