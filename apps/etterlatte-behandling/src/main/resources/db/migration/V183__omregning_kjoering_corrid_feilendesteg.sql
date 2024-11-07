ALTER TABLE omregningskjoering
    ADD COLUMN corr_id VARCHAR NOT NULL DEFAULT '';

ALTER TABLE omregningskjoering
    ADD COLUMN feilende_steg VARCHAR NOT NULL DEFAULT '';