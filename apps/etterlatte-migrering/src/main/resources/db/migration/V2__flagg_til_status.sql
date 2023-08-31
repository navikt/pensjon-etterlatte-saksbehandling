ALTER TABLE pesyssak ADD status VARCHAR not null DEFAULT 'HENTA';
ALTER TABLE pesyssak DROP COLUMN migrert;