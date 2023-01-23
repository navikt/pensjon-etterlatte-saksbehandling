ALTER TABLE beregningsperiode ADD COLUMN trygdetid BIGINT;
ALTER TABLE beregningsperiode ADD COLUMN regelResultat JSONB;
ALTER TABLE beregningsperiode ADD COLUMN regelVersjon TEXT;
