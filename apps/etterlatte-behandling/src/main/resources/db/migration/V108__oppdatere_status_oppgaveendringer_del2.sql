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
