CREATE TABLE utbetalingsoppdrag
(
    id BIGSERIAL PRIMARY KEY,
    sak_id VARCHAR NOT NULL,
    behandling_id VARCHAR NOT NULL,
    vedtak_id VARCHAR NOT NULL,
    vedtak TEXT NOT NULL,
    oppdrag TEXT NOT NULL,
    status VARCHAR NOT NULL,
    oppdrag_id VARCHAR,
    kvittering TEXT
);