ALTER TABLE beregningsperiode
  ADD COLUMN beregnings_metode TEXT,
  ADD COLUMN samlet_norsk_trygdetid BIGINT,
  ADD COLUMN samlet_teoretisk_trygdetid BIGINT,
  ADD COLUMN prorata_broek_nevner BIGINT,
  ADD COLUMN prorata_broek_teller BIGINT;

UPDATE beregningsperiode SET beregnings_metode = 'NASJONAL' WHERE beregnings_metode IS NULL;
UPDATE beregningsperiode SET samlet_norsk_trygdetid = trygdetid WHERE samlet_norsk_trygdetid IS NULL;
