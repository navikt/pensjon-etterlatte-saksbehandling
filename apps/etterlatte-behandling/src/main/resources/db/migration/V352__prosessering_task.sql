-- Prosessering (transaksjonell-outbox jobbkø) — PoC skyggekjøring av søknadsmottak.
-- Tabellen ligger i public sammen med behandlings øvrige tabeller, slik at dev-databasen
-- klones uendret lokalt (clone-dev-db.sh) uten å håndtere et ekstra skjema. Prefikset navn
-- unngår kollisjon og gjør opphavet tydelig.
--
-- Denne migreringen konsoliderer sluttilstanden fra den opprinnelige PoC-sekvensen
-- (eget prosessering-skjema → flyttet til public → sekvens/grants). IF NOT EXISTS gjør den
-- idempotent på dev der tabellen allerede finnes fra den tidligere sekvensen, samtidig som
-- ferske databaser (prod, lokalt, CI) får den opprettet fra bunnen.
CREATE TABLE IF NOT EXISTS public.prosessering_task (
    id            BIGSERIAL PRIMARY KEY,
    type          TEXT        NOT NULL,
    status        TEXT        NOT NULL DEFAULT 'KLAR',
    payload       TEXT,
    trigger_tid   TIMESTAMPTZ NOT NULL DEFAULT now(),
    opprettet_tid TIMESTAMPTZ NOT NULL DEFAULT now(),
    plukket_tid   TIMESTAMPTZ,
    antall_feil   INT         NOT NULL DEFAULT 0,
    stoppaarsak   TEXT,
    versjon       BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_prosessering_task_plukk
    ON public.prosessering_task (trigger_tid)
    WHERE status = 'KLAR';

-- Lesetilgang for personlige IAM-roller (SELECT via PUBLIC), både på tabellen og på
-- BIGSERIAL-sekvensen slik at pg_dump i clone-dev-db.sh kan lese last_value.
GRANT SELECT ON public.prosessering_task TO PUBLIC;
GRANT SELECT ON SEQUENCE public.prosessering_task_id_seq TO PUBLIC;
