ALTER TABLE IF EXISTS etteroppgjoer_beregnet_resultat
    ADD COLUMN IF NOT EXISTS vedtak_referanse TEXT,
    ALTER COLUMN referanse_avkorting_sist_iverksatte DROP NOT NULL;

ALTER TABLE IF EXISTS avkortingsgrunnlag_faktisk
    ADD COLUMN IF NOT EXISTS maaneder_innvilget JSONB,
    ADD COLUMN IF NOT EXISTS maaneder_innvilget_regel_resultat JSONB;

ALTER TABLE IF EXISTS avkortingsgrunnlag_forventet
    ADD COLUMN IF NOT EXISTS maaneder_innvilget JSONB,
    ADD COLUMN IF NOT EXISTS maaneder_innvilget_regel_resultat JSONB;

CREATE TABLE IF NOT EXISTS vedtaksperioder
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    behandling_id UUID NOT NULL,
    fra_og_med    TEXT NOT NULL,
    til_og_med    TEXT
);
