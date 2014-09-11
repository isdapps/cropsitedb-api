# ACE Metadata updated to add fl_geohash

# --- !Ups

ALTER TABLE ace_metadata ADD fl_geohash text;

# --- !Downs

ALTER TABLE ace_metadata DROP fl_geohash;
