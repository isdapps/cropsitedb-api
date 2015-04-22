# Add DOME metadata

# --- !Ups

CREATE TABLE dome_metadata (
	dsid text,
	created timestamp DEFAULT now(),
	dome_id text,
	reg_id text,
	stratum text,
	rap_id text,
	man_id text,
	rap_ver text,
	clim_id text,
	description text
);

# --- !Downs

DROP TABLE dome_metadata;
