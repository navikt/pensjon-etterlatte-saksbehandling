-- Vi vil kun se på de nyere sakene, slik at vi ikke trenger å hente hele databasen til bigquery hver gang
CREATE OR REPLACE VIEW stoenad_statistikk AS
    SELECT * FROM stoenad
    WHERE tidspunkt_registrert > DATE(now()) - interval '31 days';