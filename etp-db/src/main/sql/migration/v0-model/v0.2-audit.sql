
create or replace function etp.current_kayttaja_id() returns int as $$
begin
  return split_part(current_setting('application_name'), '@', 1) :: int;
end;
$$ language plpgsql immutable;

create or replace function etp.current_service_uri() returns text as $$
begin
  return split_part(current_setting('application_name'), '@', 2) :: text;
end;
$$ language plpgsql immutable;

/**
  Create audit table based on all columns in an auditable table.

  Auditable table is the original table,
  which version history is stored in audit table.
 */
create or replace procedure audit.create_audit_table(table_name name)
  language plpgsql
as $$
begin
  execute format(
    'create table audit.%I (
      event_id int generated by default as identity (cache 1) primary key,
      transaction_id bigint not null default txid_current(),
      modifytime timestamp with time zone not null default transaction_timestamp(),
      modifiedby_id int not null default etp.current_kayttaja_id() references kayttaja (id),
      service_uri text not null default etp.current_service_uri(),

      like etp.%I including comments
    )', table_name, table_name);
end
$$;

/**
  Find data columns of an audit table
 */
create or replace function audit.columns(audit_table_name name) returns text[]
as $$
begin
  return array(
    select column_name::text
    from information_schema.columns
    where table_schema = 'audit' and
          table_name   = audit_table_name and
          ordinal_position > 5
    order by ordinal_position);
end;
$$
language 'plpgsql';

/**
  Create or update audit procedure for an audit table.
  This is used in audit event triggers.
 */
create or replace procedure audit.create_audit_procedure(audit_table_name name)
  language plpgsql
as $$
declare
  column_names text[] := audit.columns(audit_table_name);
begin
  execute format(
    'create or replace function audit.%I() returns trigger as
    $a$
    begin
      insert into audit.%I ( %s ) values ( %s );
      return new;
    end;
    $a$
    language ''plpgsql'' security definer',
    audit_table_name || '_audit',
    audit_table_name,
    (select string_agg(v, ', ') from unnest(column_names) as v),
    (select string_agg('new.' || v, ', ') from unnest(column_names) as v));
end
$$;

create or replace procedure audit.create_audit_insert_trigger(
  audit_table_name name, target_table_name name)
  language plpgsql
as $$
begin
  execute format(
    'create trigger %I
      after insert on %I for each row
      execute procedure audit.%I()',
    audit_table_name || '_insert_trigger',
    target_table_name,
    audit_table_name || '_audit');
end
$$;

create or replace function audit.update_condition(audit_table_name name) returns text
as $$
declare
  column_names text[] := audit.columns(audit_table_name);
begin
  return format(
    '( %s ) is distinct from ( %s )',
    (select string_agg('old.' || v, ', ') from unnest(column_names) as v),
    (select string_agg('new.' || v, ', ') from unnest(column_names) as v));
end;
$$
language 'plpgsql';

create or replace procedure audit.create_audit_update_trigger(
  audit_table_name name, target_table_name name, update_condition text)
  language plpgsql
as $$
begin
  execute format(
    'create trigger %I
      after update on %I for each row
      when ( %s )
      execute procedure audit.%I()',
    audit_table_name || '_update_trigger',
    target_table_name, update_condition,
    audit_table_name || '_audit');
end
$$;

create or replace procedure audit.activate(table_name name)
  language plpgsql
as $$
begin
  call audit.create_audit_table(table_name);
  call audit.create_audit_procedure(table_name);
  call audit.create_audit_insert_trigger(table_name, table_name);
  call audit.create_audit_update_trigger(table_name, table_name,
    'old is distinct from new');
end
$$;
