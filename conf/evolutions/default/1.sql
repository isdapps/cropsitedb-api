# ACE Metadata Schema v1

# --- !Ups

CREATE TABLE ace_metadata (
    dsid text,
    created timestamp without time zone DEFAULT now(),
    eid text,
    sid text,
    wid text,
    user_id text,
    ex_distrib text,
    ex_email text,
    wst_distrib text,
    wst_email text,
    suiteid text,
    exname text,
    institution text,
    local_name text,
    data_source text,
    crid text,
    cul_name text,
    pdate date,
    hdate date,
    main_factor text,
    factors text,
    fertilizer text,
    irrig text,
    fl_lat text,
    fl_long text,
    flele text,
    fl_loc_1 text,
    fl_loc_2 text,
    fl_loc_3 text,
    hwam text,
    soil_id text,
    soil_name text,
    sl_source text,
    sltx text,
    wst_id text,
    wst_name text,
    wst_source text,
    wst_dist text,
    wst_lat text,
    wst_long text,
    wst_elev text,
    obs_end_of_season text,
    obs_time_series_data text,
    prcp text,
    tavp text,
    agmip_rating text,
    agmip_date date,
    agmip_rater text
);

# --- !Downs

DROP TABLE ace_metadata;
