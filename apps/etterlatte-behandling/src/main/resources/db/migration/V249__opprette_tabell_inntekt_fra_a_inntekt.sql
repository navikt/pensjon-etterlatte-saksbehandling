CREATE TABLE inntekt_fra_ainntekt (
    id UUID PRIMARY KEY,
    forbehandling_id UUID,
    aar INT NOT NULL,
    inntektsmaaneder JSONB NOT NULL,
    opprettet TIMESTAMP DEFAULT NOW()
);
