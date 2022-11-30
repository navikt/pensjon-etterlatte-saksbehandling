ALTER TABLE utbetaling
    ADD COLUMN saktype TEXT;

UPDATE utbetaling
SET saktype = 'BARNEPENSJON';

ALTER TABLE utbetaling
    ALTER COLUMN saktype SET NOT NULL;
