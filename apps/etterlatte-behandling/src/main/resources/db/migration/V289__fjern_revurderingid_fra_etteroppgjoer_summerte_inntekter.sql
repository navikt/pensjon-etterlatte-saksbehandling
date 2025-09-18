
ALTER TABLE etteroppgjoer_summerte_inntekter
DROP COLUMN behandling_id;

ALTER TABLE ONLY public.etteroppgjoer_summerte_inntekter
    ADD CONSTRAINT eo_summerte_inntekter_forbehandling_id UNIQUE (forbehandling_id);
