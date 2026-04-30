ALTER TABLE stoenad ADD COLUMN ytelse_foer_avkorting TEXT;
ALTER TABLE maaned_stoenad ADD COLUMN ytelse_foer_avkorting TEXT;

CREATE OR REPLACE VIEW maaned_stoenad_statistikk AS SELECT * FROM maaned_stoenad
    WHERE extract(MONTH FROM registrertTimestamp) = extract(MONTH FROM NOW());
