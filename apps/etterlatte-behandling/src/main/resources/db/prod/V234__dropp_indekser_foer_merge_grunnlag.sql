-- fordi det tar kjempelang tid å bygge disse på all data i prod, så vil
-- vi ikke ha denne tregheten innebygget i migreringen av dataen fra grunnlag til behandling
DROP INDEX IF EXISTS behandling_versjon_behandling_id_sak_id_idx;
DROP INDEX IF EXISTS grunnlagshendelse_fnr_idx;
DROP INDEX IF EXISTS grunnlagshendelse_opplysning_gin_idx;
DROP INDEX IF EXISTS grunnlagshendelse_opplysning_id_index;
DROP INDEX IF EXISTS grunnlagshendelse_opplysning_type_idx;
DROP INDEX IF EXISTS grunnlagshendelse_sak_id_idx;