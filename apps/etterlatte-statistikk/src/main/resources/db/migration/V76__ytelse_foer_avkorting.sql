ALTER TABLE stoenad ADD COLUMN ytelseFoerAvkorting TEXT;
ALTER TABLE maaned_stoenad ADD COLUMN ytelseFoerAvkorting TEXT;

CREATE OR REPLACE VIEW maaned_stoenad_statistikk AS SELECT * FROM maaned_stoenad
    WHERE extract(MONTH FROM registrertTimestamp) = extract(MONTH FROM NOW());
