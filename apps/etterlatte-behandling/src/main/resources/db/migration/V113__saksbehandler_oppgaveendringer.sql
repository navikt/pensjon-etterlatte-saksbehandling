
-- Sette { "saksbehandler": null } i tilfeller hvor gammel felt finnes og ident mangler
UPDATE oppgaveendringer
SET oppgavefoer = jsonb_set(
        (oppgavefoer::jsonb - 'saksbehandlerNavn' - 'saksbehandlerIdent'),
        '{saksbehandler}',
        'null'
                  )
WHERE (oppgavefoer ? 'saksbehandlerIdent' OR oppgavefoer ? 'saksbehandlerNavn')
  AND oppgavefoer ->> 'saksbehandlerIdent' IS NULL;

UPDATE oppgaveendringer
SET oppgaveetter = jsonb_set(
        (oppgaveetter::jsonb - 'saksbehandlerNavn' - 'saksbehandlerIdent'),
        '{saksbehandler}',
        'null'
                   )
WHERE (oppgaveetter ? 'saksbehandlerIdent' OR oppgaveetter ? 'saksbehandlerNavn')
  AND oppgaveetter ->> 'saksbehandlerIdent' IS NULL;


-- Konvertere saksbehandlerIdent og saksbehandlerNavn i saksbehandler-objekt
UPDATE oppgaveendringer
SET oppgavefoer = jsonb_set(
        (oppgavefoer::jsonb - 'saksbehandlerNavn' - 'saksbehandlerIdent'),
        '{saksbehandler}',
        json_build_object(
                'ident', oppgavefoer ->> 'saksbehandlerIdent',
                'navn', oppgavefoer ->> 'saksbehandlerNavn'
        )::jsonb)
WHERE oppgavefoer ? 'saksbehandlerIdent';

UPDATE oppgaveendringer
SET oppgaveetter = jsonb_set(
        (oppgavefoer::jsonb - 'saksbehandlerNavn' - 'saksbehandlerIdent'),
        '{saksbehandler}',
        json_build_object(
                'ident', oppgaveetter ->> 'saksbehandlerIdent',
                'navn', oppgaveetter ->> 'saksbehandlerNavn'
        )::jsonb)
WHERE oppgaveetter ? 'saksbehandlerIdent';
