CREATE TABLE viderefoert_opphoer
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dato          TEXT NOT NULL,
    behandling_id UUID UNIQUE NOT NULL,
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    vilkaar       TEXT NOT NULL,
    kilde         jsonb,
    kravdato      date,
    begrunnelse   TEXT,
    CONSTRAINT fk_behandling_id FOREIGN KEY (behandling_id) REFERENCES behandling (id)
);