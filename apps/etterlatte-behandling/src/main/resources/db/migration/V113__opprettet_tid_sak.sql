ALTER TABLE sak ADD COLUMN opprettet TIMESTAMP;

UPDATE sak as s
SET s.opprettet = (select behandling_opprettet from behandling as b where b.sak_id = s.id);
