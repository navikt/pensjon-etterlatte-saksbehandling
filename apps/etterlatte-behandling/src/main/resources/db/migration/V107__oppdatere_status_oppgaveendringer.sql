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
