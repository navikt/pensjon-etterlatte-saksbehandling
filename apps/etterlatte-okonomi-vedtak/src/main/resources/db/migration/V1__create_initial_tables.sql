CREATE TABLE utbetalingsoppdrag
(
    id BIGSERIAL PRIMARY KEY,
    sak_id VARCHAR NOT NULL,
    behandling_id VARCHAR NOT NULL,
    vedtak_id VARCHAR NOT NULL,
    vedtak TEXT NOT NULL,
    oppdrag TEXT NOT NULL,
    status VARCHAR NOT NULL,
    oppdrag_id VARCHAR, -- TODO: Fjerne denne da vi ikke får den returnert fra oppdrag
    kvittering TEXT
);