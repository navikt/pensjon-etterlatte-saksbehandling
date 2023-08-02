ALTER TABLE stoenad ADD COLUMN sak_utland TEXT;
UPDATE stoenad SET sak_utland = 'NASJONAL' WHERE sak_utland IS NULL;

ALTER TABLE maaned_stoenad ADD COLUMN sak_utland TEXT;
UPDATE maaned_stoenad SET sak_utland = 'NASJONAL' WHERE sak_utland IS NULL;