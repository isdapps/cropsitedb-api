# Add API_SOURCE to ace_metadata

# --- !Ups

ALTER TABLE ace_metadata ADD observed_vars text;
ALTER TABLE ace_metadata ADD api_source text;

# --- !Downs

ALTER TABLE ace_metadata DROP observed_vars;
ALTER TABLE ace_metadata DROP api_source;
