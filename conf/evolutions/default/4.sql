# Create Bulk loading tracking

# --- !Ups

CREATE TABLE bulk_loading (
	site text,
	last_seen timestamp
);


# --- !Downs

DROP TABLE bulk_loading;
