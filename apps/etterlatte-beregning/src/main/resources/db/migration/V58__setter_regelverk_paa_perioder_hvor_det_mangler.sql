-- Setter regelverk på alle perioder fom 2024
UPDATE beregningsperiode SET regelverk = 'REGELVERK_FOM_JAN_2024' WHERE regelverk is null AND datofom >= '2024-01-01';

-- Setter regelverk på alle perioder før 2024
UPDATE beregningsperiode SET regelverk = 'REGELVERK_TOM_DES_2023' WHERE regelverk is null AND datofom < '2024-01-01';

-- Legger på not-null constraint på feltet
ALTER TABLE beregningsperiode ALTER COLUMN regelverk SET NOT NULL;