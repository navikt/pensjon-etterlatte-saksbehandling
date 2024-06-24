ALTER TABLE overstyr_beregning ADD COLUMN kategori TEXT;

UPDATE overstyr_beregning
SET kategori = 'UKJENT_KATEGORI';