ALTER TABLE mottaker ADD COLUMN journalpost_id TEXT;

ALTER TABLE mottaker ADD COLUMN bestilling_id TEXT;

UPDATE mottaker
SET journalpost_id = b.journalpost_id,
    bestilling_id  = b.bestilling_id
FROM brev b
WHERE brev_id = b.id
  AND (b.journalpost_id IS NOT NULL OR b.bestilling_id IS NOT NULL);
