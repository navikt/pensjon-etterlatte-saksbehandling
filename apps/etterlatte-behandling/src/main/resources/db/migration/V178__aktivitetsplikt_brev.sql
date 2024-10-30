CREATE TABLE aktivitetsplikt_brevdata
(
    oppgave_id    UUID UNIQUE,
    sak_id        BIGINT NOT NULL,
    skal_sende_brev        BOOLEAN NOT NULL,
    utbetaling           BOOLEAN,
    redusert_etter_inntekt     BOOLEAN,
    CONSTRAINT fk_sak_id FOREIGN KEY (sak_id) REFERENCES sak (id),
    CONSTRAINT fk_oppgave_id FOREIGN KEY (oppgave_id) REFERENCES oppgave (id)
);