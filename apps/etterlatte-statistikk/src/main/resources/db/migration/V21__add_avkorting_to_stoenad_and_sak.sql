ALTER TABLE stoenad ADD COLUMN avkorting JSONB;
ALTER TABLE sak ADD COLUMN avkorting JSONB;

ALTER TABLE maaned_stoenad ADD COLUMN avkortingsbeloep TEXT;
ALTER TABLE maaned_stoenad ADD COLUMN aarsinntekt TEXT;