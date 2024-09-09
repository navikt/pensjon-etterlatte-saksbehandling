ALTER TABLE beregningsgrunnlag ADD COLUMN kun_en_juridisk_forelder JSONB;
ALTER TABLE beregningsperiode ADD COLUMN kun_en_juridisk_forelder BOOLEAN;
