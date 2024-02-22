alter table sak add column relatert_til text;

-- Refresh av view for sakstatistikk-import
CREATE OR REPLACE VIEW sak_statistikk AS
SELECT * FROM sak
WHERE tidspunkt_registrert > NOW() - interval '2 days';