-- Midlertidige kolonner for å enkelt se hva som var tidligere tilstand i tilfelle noe har gått skeis
ALTER TABLE oppgave
    ADD COLUMN type_backup   TEXT,
    ADD COLUMN status_backup TEXT;

UPDATE oppgave
SET type_backup   = type,
    status_backup = status
WHERE true;



----------------
-- BEHANDLING --
----------------

UPDATE oppgave
SET status = oppgave.type,
    type   = CASE
                 WHEN temp.behandlingstype = 'FØRSTEGANGSBEHANDLING' THEN 'FOERSTEGANGSBEHANDLING'
                 ELSE temp.behandlingstype
        END
FROM (SELECT o2.id, b.behandlingstype
      FROM oppgave o2
               LEFT JOIN behandling b ON o2.referanse = b.id::text
      WHERE o2.kilde = 'BEHANDLING') AS temp
WHERE oppgave.id = temp.id
  AND oppgave.type IN ('ATTESTERING', 'UNDERKJENT')
  AND oppgave.status IN ('NY', 'UNDER_BEHANDLING');


UPDATE oppgave
SET type = CASE
               WHEN behandlingstype = 'FØRSTEGANGSBEHANDLING' THEN 'FOERSTEGANGSBEHANDLING'
               ELSE behandlingstype
    END
FROM (SELECT o2.id, b.behandlingstype
      FROM oppgave o2
               LEFT JOIN behandling b ON o2.referanse = b.id::text
      WHERE o2.kilde = 'BEHANDLING') AS temp
WHERE oppgave.id = temp.id
  AND oppgave.type IN ('ATTESTERING', 'UNDERKJENT');


-------------------------
-- GENERELL_BEHANDLING --
--    TILBAKEKREVING   --
--    GJENOPPRETTING   --
-------------------------

-- Endre type og status på alle under behandling med type att./und.
UPDATE oppgave
SET status = type,
    type   = CASE
                 WHEN kilde = 'GENERELL_BEHANDLING'
                     THEN 'KRAVPAKKE_UTLAND'
                 when kilde = 'GJENOPPRETTING'
                     THEN 'FOERSTEGANGSBEHANDLING'
                 ELSE kilde
        END
WHERE kilde IN ('GENERELL_BEHANDLING', 'TILBAKEKREVING', 'GJENOPPRETTING')
  AND type IN ('ATTESTERING', 'UNDERKJENT')
  AND status IN ('NY', 'UNDER_BEHANDLING');

-- Sette type på resterende oppgaver
UPDATE oppgave
SET type = CASE
               WHEN kilde = 'GENERELL_BEHANDLING'
                   THEN 'KRAVPAKKE_UTLAND'
               when kilde = 'GJENOPPRETTING'
                   THEN 'FOERSTEGANGSBEHANDLING'
               ELSE kilde
    END
WHERE kilde IN ('GENERELL_BEHANDLING', 'TILBAKEKREVING', 'GJENOPPRETTING')
  AND type IN ('ATTESTERING', 'UNDERKJENT');

