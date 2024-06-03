ALTER TABLE overstyr_beregning ADD COLUMN status TEXT NOT NULL DEFAULT 'GYLDIG';

-- Det overstyrte beregningsgrunnlaget hører til en behandling som er avbrutt
-- Ser ut til at noen har satt den saken til overstyrt, også har de avbrutt behandlingen i etterkant.
UPDATE overstyr_beregning SET status = 'UGYLDIG' WHERE sak_id=2912;