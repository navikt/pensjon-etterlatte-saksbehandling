ALTER TABLE avstemming
ADD COLUMN avstemmingtype TEXT;


UPDATE avstemming
SET avstemmingtype = 'GRENSESNITTAVSTEMMING';

ALTER TABLE avstemming
    ALTER COLUMN avstemmingtype SET NOT NULL;

