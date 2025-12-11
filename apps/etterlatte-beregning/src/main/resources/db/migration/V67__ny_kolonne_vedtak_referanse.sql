ALTER TABLE etteroppgjoer_beregnet_resultat
    ADD COLUMN IF NOT EXISTS vedtak_referanse TEXT;

ALTER TABLE etteroppgjoer_beregnet_resultat
    ALTER COLUMN referanse_avkorting_sist_iverksatte DROP NOT NULL