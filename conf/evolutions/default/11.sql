# Add download_counter

# --- !Ups

ALTER TABLE ace_metadata ADD download_count bigint;

# --- !Downs

ALTER TABLE ace_metadata DROP download_count;
