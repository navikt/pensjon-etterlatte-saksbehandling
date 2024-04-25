ALTER TABLE sak ADD COLUMN opprettet TIMESTAMP;

with tidligste_behandling as (select min(behandling_opprettet) as opprettet, sak_id from behandling group by sak_id)

UPDATE sak as s
SET opprettet = (select opprettet from tidligste_behandling as b where b.sak_id = s.id);
