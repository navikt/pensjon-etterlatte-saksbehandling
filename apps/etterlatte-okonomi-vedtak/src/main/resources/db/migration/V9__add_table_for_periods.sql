ALTER TABLE utbetalingsoppdrag RENAME TO utbetaling;
ALTER TABLE utbetaling ALTER COLUMN id TYPE VARCHAR;
ALTER TABLE utbetaling ALTER COLUMN sak_id TYPE BIGINT USING sak_id::bigint;
ALTER TABLE utbetaling ALTER COLUMN vedtak_id TYPE BIGINT USING vedtak_id::bigint;
ALTER TABLE utbetaling ADD COLUMN saksbehandler VARCHAR(32) NOT NULL;
ALTER TABLE utbetaling ADD COLUMN attestant VARCHAR(32) NOT NULL;

CREATE TABLE utbetalingslinje (
    id                      BIGINT                      PRIMARY KEY,
    type                    VARCHAR                     NOT NULL,
    utbetaling_id           VARCHAR                     REFERENCES utbetaling(id),
    erstatter_id            BIGINT                      DEFAULT NULL,
    opprettet               TIMESTAMP WITH TIME ZONE    NOT NULL,
    sak_id                  BIGINT                      NOT NULL,
    periode_fra             DATE                        NOT NULL,
    periode_til             DATE                        ,
    beloep                  NUMERIC                     NOT NULL
);