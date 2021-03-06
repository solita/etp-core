
alter table etp.energiatodistus
  add column valvonta$pending boolean not null default false,
  add column valvonta$valvoja_id int references etp.kayttaja (id);
call audit.create_audit_procedure('energiatodistus'::name);

call create_classification('vo_toimenpidetype'::name);

call create_classification('vo_template'::name);
alter table vo_template
  add column toimenpidetype_id int not null references vo_toimenpidetype (id),
  add column language text not null,
  add column content text;

call create_classification('vo_virhetype'::name);
alter table vo_virhetype
  drop column description,
  add column description_fi text not null,
  add column description_sv text not null;

call create_classification('vo_severity'::name);

call audit.activate('vo_toimenpidetype'::name);
call audit.activate('vo_template'::name);
call audit.activate('vo_virhetype'::name);
call audit.activate('vo_severity'::name);

create table vo_toimenpide (
  id int generated by default as identity primary key,
  type_id int not null references vo_toimenpidetype (id),
  author_id int not null default etp.current_kayttaja_id() references etp.kayttaja (id),
  energiatodistus_id int not null references etp.energiatodistus (id),

  create_time timestamp with time zone not null default transaction_timestamp(),
  publish_time timestamp with time zone,
  deadline_date date,
  template_id int references vo_template (id),
  diaarinumero text,
  description text,
  severity_id int references vo_severity (id)
);
call audit.activate('vo_toimenpide'::name);

create table vo_virhe (
  toimenpide_id int not null references vo_toimenpide (id),
  type_id       int not null references vo_virhetype (id),
  description   text,

  primary key (toimenpide_id, type_id)
);
