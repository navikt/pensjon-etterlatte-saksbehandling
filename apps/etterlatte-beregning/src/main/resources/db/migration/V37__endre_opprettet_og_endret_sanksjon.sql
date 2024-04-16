ALTER TABLE sanksjon ALTER COLUMN endret TYPE text USING endret::text;

ALTER TABLE sanksjon
    ALTER COLUMN opprettet TYPE text USING opprettet::text;

ALTER TABLE sanksjon
    DROP COLUMN saksbehandler;