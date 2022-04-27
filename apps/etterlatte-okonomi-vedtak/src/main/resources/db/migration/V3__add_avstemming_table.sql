TRUNCATE TABLE utbetalingsoppdrag;

ALTER TABLE utbetalingsoppdrag
ADD COLUMN opprettet_tidspunkt TIMESTAMP WITH TIME ZONE NOT NULL,
ADD COLUMN endret TIMESTAMP WITH TIME ZONE NOT NULL,
ADD COLUMN fodselsnummer VARCHAR(32),
ADD COLUMN beskrivelse_oppdrag TEXT NULL DEFAULT NULL,
ADD COLUMN feilkode_oppdrag VARCHAR(32) NULL DEFAULT NULL,
ADD COLUMN melding_kode_oppdrag VARCHAR(32) NULL DEFAULT NULL;

ALTER TABLE utbetalingsoppdrag
RENAME COLUMN oppdrag TO utgaende_oppdrag;
ALTER TABLE utbetalingsoppdrag
RENAME COLUMN kvittering TO oppdrag_kvittering;


CREATE TABLE avstemming (
    id                      UUID                     NOT NULL,
    opprettet               TIMESTAMP WITH TIME ZONE NOT NULL,
   fagomrade               VARCHAR(32)              NOT NULL,
    avstemmingsnokkel_tom   BIGINT                   NOT NULL,
    antall_avstemte_oppdrag INT                      NOT NULL,
    CONSTRAINT pk_avstemming PRIMARY KEY (id)
);