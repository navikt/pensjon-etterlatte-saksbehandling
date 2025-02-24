CREATE EXTENSION IF NOT EXISTS btree_gin WITH SCHEMA public;
COMMENT ON EXTENSION btree_gin IS 'support for indexing common datatypes in GIN';

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;
COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


CREATE INDEX behandling_versjon_behandling_id_sak_id_idx
    ON public.behandling_versjon USING btree (behandling_id, sak_id);

CREATE INDEX grunnlagshendelse_fnr_idx
    ON public.grunnlagshendelse USING btree (fnr);

CREATE INDEX grunnlagshendelse_opplysning_gin_idx
    ON public.grunnlagshendelse USING gin (opplysning public.gin_trgm_ops);

CREATE INDEX grunnlagshendelse_opplysning_id_index
    ON public.grunnlagshendelse USING btree (opplysning_id);

CREATE INDEX grunnlagshendelse_opplysning_type_idx
    ON public.grunnlagshendelse USING btree (opplysning_type);

CREATE INDEX grunnlagshendelse_sak_id_idx
    ON public.grunnlagshendelse USING btree (sak_id);
