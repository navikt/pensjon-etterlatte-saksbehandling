-- Refresh av view for forrige maanedes statistikk for import
--
-- Statistikk for hva som ble utbetalt forrige måned produseres denne måneden,
-- så alle rader som er opprettet denne måneden gjelder statistikk for forrige måned
CREATE OR REPLACE VIEW maaned_stoenad_statistikk AS
SELECT *
FROM maaned_stoenad
WHERE extract(MONTH FROM registrertTimestamp) = extract(MONTH FROM NOW());