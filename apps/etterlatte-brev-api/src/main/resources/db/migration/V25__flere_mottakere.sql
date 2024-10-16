ALTER TABLE mottaker
    DROP CONSTRAINT mottaker_pkey;

ALTER TABLE mottaker
    ADD COLUMN id UUID PRIMARY KEY DEFAULT (gen_random_uuid());

-- Nytt felt for type mottaker (HOVED eller KOPI)
ALTER TABLE mottaker ADD COLUMN type TEXT;
UPDATE mottaker SET type = 'HOVED';
ALTER TABLE mottaker ALTER COLUMN type SET NOT NULL;
