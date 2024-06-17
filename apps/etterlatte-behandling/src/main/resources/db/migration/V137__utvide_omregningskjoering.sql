ALTER TABLE omregningskjoering ADD COLUMN beregning_beloep_foer bigint null default null;
ALTER TABLE omregningskjoering ADD COLUMN beregning_beloep_etter bigint null default null;
ALTER TABLE omregningskjoering ADD COLUMN beregning_g_foer bigint null default null;
ALTER TABLE omregningskjoering ADD COLUMN beregning_g_etter bigint null default null;
ALTER TABLE omregningskjoering ADD COLUMN beregning_brukt_omregningsfaktor bigint null default null;
ALTER TABLE omregningskjoering ADD COLUMN vedtak_beloep bigint null default null;