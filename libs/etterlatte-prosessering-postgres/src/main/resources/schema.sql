CREATE TABLE IF NOT EXISTS task (
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

CREATE INDEX IF NOT EXISTS idx_task_plukk
    ON task (trigger_tid)
    WHERE status = 'KLAR';

-- Brukes av konkurranse-testen: en UNIQUE-constraint på task_id gjør at en
-- dobbel-eksekvering av samme task ville kaste en constraint-violation.
CREATE TABLE IF NOT EXISTS execution_log (
    task_id    BIGINT      NOT NULL,
    node       TEXT        NOT NULL,
    kjort_tid  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_execution_log_task UNIQUE (task_id)
);
