ALTER TABLE beregningsperiode ADD COLUMN type TEXT;
UPDATE beregningsperiode SET type = 'BP';