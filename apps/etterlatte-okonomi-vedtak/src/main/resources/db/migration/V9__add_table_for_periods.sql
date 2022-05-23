ALTER TABLE utbetalingsoppdrag RENAME TO utbetaling;
ALTER TABLE utbetaling ALTER COLUMN id TYPE VARCHAR;
ALTER TABLE utbetaling ALTER COLUMN sak_id TYPE BIGINT USING sak_id::bigint;
ALTER TABLE utbetaling ALTER COLUMN vedtak_id TYPE BIGINT USING vedtak_id::bigint;


ALTER TABLE utbetaling ADD COLUMN saksbehandler VARCHAR(32) NOT NULL;
ALTER TABLE utbetaling ADD COLUMN attestant VARCHAR(32) NOT NULL;

CREATE TABLE utbetalingslinje (
    id                      BIGINT                      PRIMARY KEY,
    opprettet               TIMESTAMP WITH TIME ZONE    NOT NULL,
    periode_fra             DATE                        NOT NULL,
    periode_til             DATE                        ,
    beloep                  NUMERIC                     NOT NULL,
    utbetaling_id           VARCHAR                     REFERENCES utbetaling(id),
    erstatter_id            BIGINT                      DEFAULT NULL,
    sak_id                  BIGINT                      NOT NULL
);


-- id  opprettet   periode_fra     periode_til     beloep   utbetaling_id  erstatter_id    sak_id
-- 1   01.01.22    01 22                           10000    1                              1
-- 2   01.04.22    05 22                           5000     2              1               1
-- 3   01.01.23    02 23           05 23           7000     3              2               1
-- 4   01.01.23    06 23                           3000     3              2               1

-- SELECT * from utbetalingslinje as ul, utbetaling as u
-- INNER JOIN ON u.id == ul.id
-- WHERE u.sak_id == ?

--  SELECT * from utbetalingslinje WHERE sak_id == 1;
