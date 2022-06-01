CREATE TABLE utbetaling (
    id                          VARCHAR PRIMARY KEY,
    sak_id                      BIGINT NOT NULL,
    behandling_id               VARCHAR NOT NULL,
    vedtak_id                   BIGINT UNIQUE NOT NULL,
    status                      VARCHAR NOT NULL,
    opprettet                   TIMESTAMP WITH TIME ZONE NOT NULL,
    endret                      TIMESTAMP WITH TIME ZONE NOT NULL,
    avstemmingsnoekkel          TIMESTAMP WITH TIME ZONE NOT NULL,
    stoenadsmottaker            VARCHAR(32) NOT NULL,
    saksbehandler               VARCHAR(32) NOT NULL,
    attestant                   VARCHAR(32) NOT NULL,
    vedtak                      TEXT NOT NULL,
    oppdrag                     TEXT NOT NULL,
    kvittering                  TEXT NULL DEFAULT NULL,
    kvittering_beskrivelse      VARCHAR NULL DEFAULT NULL,
    kvittering_alvorlighetsgrad VARCHAR(32) NULL DEFAULT NULL,
    kvittering_kode             VARCHAR(32) NULL DEFAULT NULL
);

CREATE TABLE utbetalingslinje (
    id                          BIGINT PRIMARY KEY,
    utbetaling_id               VARCHAR REFERENCES utbetaling(id),
    erstatter_id                BIGINT DEFAULT NULL,
    sak_id                      BIGINT NOT NULL,
    type                        VARCHAR NOT NULL,
    opprettet                   TIMESTAMP WITH TIME ZONE NOT NULL,
    periode_fra                 DATE NOT NULL,
    periode_til                 DATE,
    beloep                      NUMERIC NOT NULL
);

CREATE TABLE avstemming (
    id                          VARCHAR(32) PRIMARY KEY,
    opprettet                   TIMESTAMP WITH TIME ZONE NOT NULL,
    periode_fra                 TIMESTAMP WITH TIME ZONE NOT NULL,
    periode_til                 TIMESTAMP WITH TIME ZONE NOT NULL,
    antall_oppdrag              INT NOT NULL,
    avstemmingsdata             TEXT NULL DEFAULT NULL
);