# Add support for upload progress

# --- !Ups

ALTER TABLE ace_datasets ADD COLUMN state text DEFAULT 'create';

# --- !Downs

ALTER TABLE ace_dataset DROP COLUMN state;
