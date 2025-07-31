ALTER TABLE vedtak
    DROP COLUMN datoiverksatt;

ALTER TABLE vedtak
    ADD COLUMN datoiverksatt TIMESTAMP WITH TIME ZONE;
