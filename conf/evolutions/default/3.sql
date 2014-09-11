# Create host-based API calls

# --- !Ups

CREATE TABLE extern_hosts (
       host text,
       site text,
       key  text
);


# --- !Downs

DROP TABLE extern_hosts;
