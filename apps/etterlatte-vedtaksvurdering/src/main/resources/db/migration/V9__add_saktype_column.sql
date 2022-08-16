ALTER TABLE vedtak
    ADD COLUMN saktype TEXT;
UPDATE vedtak SET saktype = 'BARNEPENSJON';