# Add ACMO Metadata

# --- !Ups

CREATE table alink_metadata (
	dsid text,
	created timestamp DEFAULT now(),
	alink_file text
);

# --- !Downs

DROP TABLE alink_metadata;
