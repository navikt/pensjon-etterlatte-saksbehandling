ALTER TABLE overstyr_beregning ADD COLUMN kategori TEXT

UPDATE overstyrt_beregning SET kategori = "UKJENT_KATEGORI"