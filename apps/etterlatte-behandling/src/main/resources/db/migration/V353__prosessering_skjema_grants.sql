-- V352 opprettet skjemaet `prosessering`, men uten grants. Et nytt skjema gir kun
-- eieren (Flyway/app-brukeren) tilgang, mens `public`-skjemaet på POSTGRES_14 lener
-- seg på PostgreSQLs default PUBLIC-privilegier. Motoren kjører derfor fint (den er
-- eier), men lesende roller (personlige IAM-brukere) får «permission denied for schema
-- prosessering». Vi replikerer public sitt mønster så PoC-tasken kan inspiseres.
GRANT USAGE ON SCHEMA prosessering TO PUBLIC;
GRANT SELECT ON ALL TABLES IN SCHEMA prosessering TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA prosessering GRANT SELECT ON TABLES TO PUBLIC;
