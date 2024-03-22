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


----------------------
-- OPPGAVEENDRINGER --
----------------------


-- TILBAKEKREVING, GENERELL_BEHANDLING, og GJENOPPRETTING
-- oppgavefoer
UPDATE oppgaveendringer
SET oppgavefoer = jsonb_set(oppgavefoer::JSONB, '{status}', (
    CASE
        WHEN oppgavefoer ->> 'type' = 'GJENOPPRETTING'
            THEN '"FOERSTEGANGSBEHANDLING"'::JSONB
        ELSE oppgavefoer -> 'type' END
    ))
WHERE oppgavefoer ->> 'kilde' IN ('GENERELL_BEHANDLING', 'TILBAKEKREVING', 'GJENOPPRETTING')
  AND oppgavefoer ->> 'type' IN ('ATTESTERING', 'UNDERKJENT')
  AND oppgavefoer ->> 'status' IN ('NY', 'UNDER_BEHANDLING');

UPDATE oppgaveendringer
SET oppgavefoer = jsonb_set(oppgavefoer::JSONB, '{type}', (
    CASE
        WHEN oppgavefoer ->> 'kilde' = 'GENERELL_BEHANDLING'
            THEN '"KRAVPAKKE_UTLAND"'::JSONB
        WHEN oppgavefoer ->> 'kilde' = 'GJENOPPRETTING'
            THEN '"FOERSTEGANGSBEHANDLING"'::JSONB
        ELSE oppgavefoer -> 'kilde'
        END
    ))
WHERE oppgavefoer ->> 'kilde' IN ('GENERELL_BEHANDLING', 'TILBAKEKREVING', 'GJENOPPRETTING');

-- TILBAKEKREVING, GENERELL_BEHANDLING, og GJENOPPRETTING
-- oppgaveetter
UPDATE oppgaveendringer
SET oppgaveetter = jsonb_set(oppgaveetter::JSONB, '{status}', (
    CASE
        WHEN oppgaveetter ->> 'type' = 'GJENOPPRETTING'
            THEN '"FOERSTEGANGSBEHANDLING"'::JSONB
        ELSE oppgaveetter -> 'type' END
    ))
WHERE oppgaveetter ->> 'kilde' IN ('GENERELL_BEHANDLING', 'TILBAKEKREVING', 'GJENOPPRETTING')
  AND oppgaveetter ->> 'type' IN ('ATTESTERING', 'UNDERKJENT')
  AND oppgaveetter ->> 'status' IN ('NY', 'UNDER_BEHANDLING');

UPDATE oppgaveendringer
SET oppgaveetter = jsonb_set(oppgaveetter::JSONB, '{type}', (
    CASE
        WHEN oppgaveetter ->> 'kilde' = 'GENERELL_BEHANDLING'
            THEN '"KRAVPAKKE_UTLAND"'::JSONB
        WHEN oppgaveetter ->> 'kilde' = 'GJENOPPRETTING'
            THEN '"FOERSTEGANGSBEHANDLING"'::JSONB
        ELSE oppgaveetter -> 'kilde'
        END
    ))
WHERE oppgaveendringer.oppgaveetter ->> 'kilde' IN ('GENERELL_BEHANDLING', 'TILBAKEKREVING', 'GJENOPPRETTING');


-- Behandling (FOERSTEGANGSBEHANDLING, REVURDERING)
-- oppgavefoer
UPDATE oppgaveendringer
SET oppgavefoer = jsonb_set(oppgavefoer::JSONB, '{status}', oppgavefoer -> 'type')
FROM (SELECT o2.id,
             (
                 CASE
                     WHEN b.behandlingstype = 'FØRSTEGANGSBEHANDLING'
                         THEN 'FOERSTEGANGSBEHANDLING'
                     ELSE b.behandlingstype
                     END
                 )
      FROM oppgaveendringer o2
               LEFT JOIN behandling b ON b.id::text = o2.oppgavefoer ->> 'referanse'
      WHERE o2.oppgavefoer ->> 'kilde' = 'BEHANDLING') AS temp
WHERE oppgaveendringer.id = temp.id
  AND oppgaveendringer.oppgavefoer ->> 'type' IN ('ATTESTERING', 'UNDERKJENT')
  AND oppgaveendringer.oppgavefoer ->> 'status' IN ('NY', 'UNDER_BEHANDLING');


UPDATE oppgaveendringer
SET oppgavefoer = jsonb_set(oppgavefoer::JSONB, '{type}', ('"' || temp.behandlingstype || '"')::jsonb)
FROM (SELECT o2.id,
             (CASE
                  WHEN b.behandlingstype = 'FØRSTEGANGSBEHANDLING'
                      THEN 'FOERSTEGANGSBEHANDLING'
                  ELSE b.behandlingstype
                 END)
      FROM oppgaveendringer o2
               LEFT JOIN behandling b ON b.id::text = o2.oppgavefoer ->> 'referanse'
      WHERE o2.oppgavefoer ->> 'kilde' = 'BEHANDLING') AS temp
WHERE oppgaveendringer.id = temp.id;


-- Behandling (FOERSTEGANGSBEHANDLING, REVURDERING)
-- oppgaveetter
UPDATE oppgaveendringer
SET oppgaveetter = jsonb_set(oppgaveetter::JSONB, '{status}', oppgaveetter -> 'type')
FROM (SELECT o2.id,
             (CASE
                  WHEN b.behandlingstype = 'FØRSTEGANGSBEHANDLING'
                      THEN 'FOERSTEGANGSBEHANDLING'
                  ELSE b.behandlingstype
                 END)
      FROM oppgaveendringer o2
               LEFT JOIN behandling b ON b.id::text = o2.oppgaveetter ->> 'referanse'
      WHERE o2.oppgaveetter ->> 'kilde' = 'BEHANDLING') AS temp
WHERE oppgaveendringer.id = temp.id
  AND oppgaveendringer.oppgaveetter ->> 'type' IN ('ATTESTERING', 'UNDERKJENT')
  AND oppgaveendringer.oppgaveetter ->> 'status' IN ('NY', 'UNDER_BEHANDLING');


UPDATE oppgaveendringer
SET oppgaveetter = jsonb_set(oppgaveetter::JSONB, '{type}', ('"' || temp.behandlingstype || '"')::jsonb)
FROM (SELECT o2.id,
             (CASE
                  WHEN b.behandlingstype = 'FØRSTEGANGSBEHANDLING'
                      THEN 'FOERSTEGANGSBEHANDLING'
                  ELSE b.behandlingstype
                 END)
      FROM oppgaveendringer o2
               LEFT JOIN behandling b ON b.id::text = o2.oppgaveetter ->> 'referanse'
      WHERE o2.oppgaveetter ->> 'kilde' = 'BEHANDLING') AS temp
WHERE oppgaveendringer.id = temp.id;


-- Mappe saksbehandler til json-objekt de stedene det er string
UPDATE oppgaveendringer
SET oppgavefoer = jsonb_set(oppgavefoer::JSONB, '{saksbehandler}',
                            json_build_object('ident', oppgavefoer ->> 'saksbehandler', 'navn',
                                              oppgavefoer ->> 'saksbehandler')::JSONB
                  )
WHERE id IN (SELECT id
             FROM oppgaveendringer
             WHERE oppgavefoer ->> 'saksbehandler' IS NOT NULL
               AND oppgavefoer ->> 'saksbehandler' NOT LIKE '%{%')
;

UPDATE oppgaveendringer
SET oppgaveetter = jsonb_set(oppgaveetter::JSONB, '{saksbehandler}',
                             json_build_object('ident', oppgaveetter ->> 'saksbehandler', 'navn',
                                               oppgaveetter ->> 'saksbehandler')::JSONB
                   )
WHERE id IN (SELECT id
             FROM oppgaveendringer
             WHERE oppgaveetter ->> 'saksbehandler' IS NOT NULL
               AND oppgaveetter ->> 'saksbehandler' NOT LIKE '%{%');
