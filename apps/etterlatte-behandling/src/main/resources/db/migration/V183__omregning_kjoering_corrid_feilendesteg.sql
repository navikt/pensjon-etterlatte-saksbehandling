ALTER TABLE omregningskjoering
    ADD COLUMN corr_id VARCHAR;

ALTER TABLE omregningskjoering
    ADD COLUMN feilende_steg VARCHAR;

ALTER TABLE omregningskjoering
    ALTER begrunnelse DROP NOT NULL;