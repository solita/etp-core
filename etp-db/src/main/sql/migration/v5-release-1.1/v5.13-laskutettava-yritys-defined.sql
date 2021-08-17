alter table etp.energiatodistus
  add column laskutettava_yritys_defined boolean not null default false;

alter table audit.energiatodistus
  add column laskutettava_yritys_defined boolean not null default false;
call audit.create_audit_procedure('energiatodistus'::name);
