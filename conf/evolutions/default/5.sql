# Create "pretty" datasets

# --- !Ups

CREATE TABLE ace_datasets(
       dsid text,
       title text,
       email text,
       frozen boolean DEFAULT FALSE,
       created timestamp without time zone DEFAULT now(),
       updated timestamp without time zone DEFAULT now()
);


# --- !Downs

DROP TABLE ace_datasets;
