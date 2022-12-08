CREATE UNIQUE INDEX behandlingId
    ON vedtak(behandlingId);

ALTER TABLE vedtak ALTER COLUMN sakId DROP NOT NULL;
