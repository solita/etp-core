
grant select on all tables in schema etp to etp_app;
grant usage on schema etp to etp_app;

grant insert, update on table kayttaja to etp_app;
grant insert, update on table laatija to etp_app;
grant insert, update on table energiatodistus to etp_app;
grant insert, update on table yritys to etp_app;
grant insert, update, delete on table laatija_yritys to etp_app;
