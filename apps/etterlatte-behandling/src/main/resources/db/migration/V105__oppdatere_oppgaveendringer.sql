
-- Opprette kolonner i stedet
ALTER TABLE oppgaveendringer
    ADD COLUMN saksbehandler TEXT,
    ADD COLUMN status        TEXT,
    ADD COLUMN merknad       TEXT,
    ADD COLUMN frist         TEXT,
    ADD COLUMN enhet         TEXT,
    ADD COLUMN kilde         TEXT,
    ADD COLUMN type          TEXT;


UPDATE oppgaveendringer
SET saksbehandler = to_json(oppgaveetter) #>> '{saksbehandler}',
    status        = to_json(oppgaveetter) #>> '{status}',
    merknad       = to_json(oppgaveetter) #>> '{merknad}',
    frist         = to_json(oppgaveetter) #>> '{frist}',
    enhet         = to_json(oppgaveetter) #>> '{enhet}',
    kilde         = to_json(oppgaveetter) #>> '{kilde}',
    type          = CASE
                        WHEN oppgavefoer::JSONB ->> 'saksbehandler' IS NOT NULL AND
                             oppgaveetter::JSONB ->> 'saksbehandler' IS NULL
                            THEN 'FJERN_TILDELING'
                        WHEN oppgavefoer::JSONB ->> 'saksbehandler' IS DISTINCT FROM oppgaveetter::JSONB ->> 'saksbehandler'
                            THEN 'ENDRE_TILDELING'
                        WHEN oppgavefoer::JSONB ->> 'status' IS DISTINCT FROM oppgaveetter::JSONB ->> 'status'
                            AND oppgavefoer::JSONB ->> 'merknad' IS DISTINCT FROM oppgaveetter::JSONB ->> 'merknad'
                            THEN 'ENDRET_STATUS_OG_MERKNAD'
                        WHEN oppgavefoer::JSONB ->> 'status' IS DISTINCT FROM oppgaveetter::JSONB ->> 'status'
                            THEN 'ENDRET_STATUS'
                        WHEN oppgavefoer::JSONB ->> 'merknad' IS DISTINCT FROM oppgaveetter::JSONB ->> 'merknad'
                            THEN 'ENDRET_MERKNAD'
                        WHEN oppgavefoer::JSONB ->> 'merknad' IS DISTINCT FROM oppgaveetter::JSONB ->> 'merknad'
                            THEN 'ENDRET_MERKNAD'
                        WHEN oppgavefoer::JSONB ->> 'frist' IS DISTINCT FROM oppgaveetter::JSONB ->> 'frist'
                            THEN 'ENDRET_FRIST'
                        WHEN oppgavefoer::JSONB ->> 'enhet' IS DISTINCT FROM oppgaveetter::JSONB ->> 'enhet'
                            THEN 'ENDRET_ENHET'
                        WHEN oppgavefoer::JSONB ->> 'kilde' IS DISTINCT FROM oppgaveetter::JSONB ->> 'kilde'
                            THEN 'ENDRET_KILDE'
                        ELSE
                            null
        END
;

UPDATE oppgaveendringer
SET saksbehandler = to_json(saksbehandler::JSONB) #>> '{ident}'
WHERE saksbehandler LIKE '{%}';

ALTER TABLE oppgaveendringer
    DROP COLUMN oppgavefoer,
    DROP COLUMN oppgaveetter;
