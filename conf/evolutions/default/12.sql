# Add bulk_loading to extern_hosts (development switch)

# --- !Ups

ALTER TABLE extern_hosts ADD bulk_loading smallint;

# --- !Downs

ALTER TABLE extern_hosts DROP bulk_loading;
