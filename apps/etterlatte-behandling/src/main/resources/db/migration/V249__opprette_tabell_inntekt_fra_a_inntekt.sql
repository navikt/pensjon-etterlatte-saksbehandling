CREATE TABLE inntekt_fra_ainntekt (
    id UUID PRIMARY KEY,
    forbehandling_id UUID,
    maaned TEXT NOT NULL,
    inntekter TEXT NOT NULL,
    summert_beloep BIGINT NOT NULL,
    opprettet TIMESTAMP DEFAULT NOW()
);
