-- Legger til nye kolonner for status og jms_timestamp
ALTER TABLE tilbakekreving_hendelse ADD COLUMN status TEXT;
ALTER TABLE tilbakekreving_hendelse ADD COLUMN jms_timestamp TIMESTAMP WITH TIME ZONE;

-- Migrerer eksisterende rader med verdier for nye kolonner
UPDATE tilbakekreving_hendelse SET status = 'FERDIGSTILT' WHERE status IS NULL;
UPDATE tilbakekreving_hendelse t SET jms_timestamp = (SELECT opprettet FROM tilbakekreving_hendelse WHERE id = t.id) WHERE jms_timestamp IS NULL AND (type = 'KRAVGRUNNLAG_MOTTATT' OR type = 'KRAV_VEDTAK_STATUS_MOTTATT');

-- Setter påkrevet constraint for status
    ALTER TABLE tilbakekreving_hendelse ALTER COLUMN status SET NOT NULL;

-- Fjerner ubrukt kolonne for kravgrunnlag_id
ALTER TABLE tilbakekreving_hendelse DROP COLUMN kravgrunnlag_id;
