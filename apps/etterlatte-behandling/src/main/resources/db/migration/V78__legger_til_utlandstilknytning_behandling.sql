ALTER TABLE behandling ADD COLUMN utlandstilknytning JSONB;

-- Oppdaterer utlandstilknytning på alle behandlinger basert på verdi i sak
UPDATE behandling
SET utlandstilknytning = sakUtlandstilknytning.utenlandstilknytning::jsonb
FROM (SELECT utenlandstilknytning, id FROM sak WHERE utenlandstilknytning IS NOT NULL) AS sakUtlandstilknytning
WHERE sakUtlandstilknytning.id = sak_id
AND behandling.status != 'AVBRUTT';