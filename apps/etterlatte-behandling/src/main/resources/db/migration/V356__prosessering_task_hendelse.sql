-- Speiler prosessering_task_hendelse fra navikt/efterlatte-prosessering#25 (ikke publisert
-- ennå). STATUS_ENDRET skrives etter hvert av motoren selv, KOMMENTAR/AVVIK av vårt eget
-- admin-API (ProsesseringModule.kt).
CREATE TABLE IF NOT EXISTS public.prosessering_task_hendelse (
    id         BIGSERIAL   PRIMARY KEY,
    task_id    BIGINT      NOT NULL,
    type       TEXT        NOT NULL,
    melding    TEXT        NOT NULL,
    endret_av  TEXT        NOT NULL,
    node       TEXT        NOT NULL,
    tidspunkt  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_prosessering_task_hendelse_task
    ON public.prosessering_task_hendelse (task_id, tidspunkt);

-- Lesetilgang for personlige IAM-roller, samme mønster som V352__prosessering_task.sql.
GRANT SELECT ON public.prosessering_task_hendelse TO PUBLIC;
GRANT SELECT ON SEQUENCE public.prosessering_task_hendelse_id_seq TO PUBLIC;
