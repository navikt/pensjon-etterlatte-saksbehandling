ALTER TABLE omregningskjoering ADD COLUMN innvilgede_perioder_foer JSONB null default null;
ALTER TABLE omregningskjoering ADD COLUMN innvilgede_perioder_etter JSONB null default null;
ALTER TABLE omregningskjoering ADD COLUMN vedtak_beloep_foer bigint null default null;
ALTER TABLE omregningskjoering ADD COLUMN vedtak_beloep_etter bigint null default null;
ALTER TABLE omregningskjoering ADD COLUMN vedtak_opphoer_foer JSONB null default null;
ALTER TABLE omregningskjoering ADD COLUMN vedtak_opphoer_etter JSONB null default null;
ALTER TABLE omregningskjoering ADD COLUMN behandling_id UUID null default null;
ALTER TABLE omregningskjoering ADD COLUMN siste_iverksatte_behandling_id UUID null default null;
