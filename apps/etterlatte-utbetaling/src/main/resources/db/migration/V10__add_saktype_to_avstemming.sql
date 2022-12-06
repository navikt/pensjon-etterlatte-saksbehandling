ALTER TABLE avstemming
    ADD COLUMN saktype TEXT;


UPDATE avstemming
SET saktype = 'BARNEPENSJON';

ALTER TABLE avstemming
    ALTER COLUMN saktype SET NOT NULL;

