CREATE TABLE utbetalingsperiode
(
    id BIGSERIAL PRIMARY KEY,
    vedtakId BIGINT NOT NULL,
    datoFom DATE NOT NULL,
    datoTom DATE,
    type TEXT NOT NULL,
    beloep NUMERIC
);