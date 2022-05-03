TRUNCATE TABLE utbetalingsoppdrag;

ALTER TABLE utbetalingsoppdrag
ADD COLUMN opprettet TIMESTAMP WITH TIME ZONE NOT NULL,
ADD COLUMN avstemmingsnoekkel TIMESTAMP WITH TIME ZONE NOT NULL,
ADD COLUMN endret TIMESTAMP WITH TIME ZONE NOT NULL,
ADD COLUMN foedselsnummer VARCHAR(32),
ADD COLUMN beskrivelse_oppdrag TEXT NULL DEFAULT NULL,
ADD COLUMN feilkode_oppdrag VARCHAR(32) NULL DEFAULT NULL,
ADD COLUMN meldingkode_oppdrag VARCHAR(32) NULL DEFAULT NULL,
DROP COLUMN oppdrag_id;

ALTER TABLE utbetalingsoppdrag
RENAME COLUMN oppdrag TO utgaaende_oppdrag;
ALTER TABLE utbetalingsoppdrag
RENAME COLUMN kvittering TO oppdrag_kvittering;


CREATE TABLE avstemming (
    id                      VARCHAR(32)                PRIMARY KEY,
    opprettet               TIMESTAMP WITH TIME ZONE NOT NULL,
    fra_og_med              TIMESTAMP WITH TIME ZONE NOT NULL,
    til                     TIMESTAMP WITH TIME ZONE NOT NULL,
    antall_avstemte_oppdrag INT                      NOT NULL
);