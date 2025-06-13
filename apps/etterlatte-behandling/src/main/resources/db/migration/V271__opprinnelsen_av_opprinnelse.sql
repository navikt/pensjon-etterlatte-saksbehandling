ALTER TABLE behandling
    RENAME COLUMN kilde TO vedtaksloesning;
ALTER TABLE behandling
    ADD COLUMN opprinnelse TEXT;
UPDATE behandling
SET opprinnelse = 'UKJENT'
WHERE opprinnelse IS NULL;