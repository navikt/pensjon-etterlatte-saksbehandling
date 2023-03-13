CREATE TABLE soeknad_statistikk (
    soeknad_id BIGINT PRIMARY KEY,
    gyldig_for_behandling BOOLEAN NOT NULL,
    saktype TEXT NOT NULL,
    kriterier_for_ingen_behandling JSONB NOT NULL,
    created_timestamp TIMESTAMP DEFAULT NOW()
);
