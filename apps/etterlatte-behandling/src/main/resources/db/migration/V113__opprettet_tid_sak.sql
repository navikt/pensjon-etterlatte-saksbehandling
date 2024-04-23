ALTER TABLE sak ADD COLUMN opprettet TIMESTAMP;

UPDATE sak as s
SET opprettet = (select behandling_opprettet from behandling as b where b.sak_id = s.id);

UPDATE sak
SET opprettet = now()::timestamp
WHERE sak.opprettet IS NULL;