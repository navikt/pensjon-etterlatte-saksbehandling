ALTER TABLE bp_beregningsgrunnlag ADD COLUMN institusjonsopphold TEXT;

UPDATE bp_beregningsgrunnlag SET institusjonsopphold = '{"institusjonsopphold":false}'
