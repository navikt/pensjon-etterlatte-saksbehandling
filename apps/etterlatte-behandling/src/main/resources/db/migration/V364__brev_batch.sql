CREATE TABLE brev_batch (
    id UUID PRIMARY KEY,
    innsender TEXT NOT NULL,
    idempotensnoekkel UUID NOT NULL,
    fingeravtrykk TEXT NOT NULL,
    request TEXT NOT NULL,
    opprettet TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (innsender, idempotensnoekkel)
);

CREATE TABLE brev_batch_sak (
    utsendings_id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES brev_batch(id),
    sak_id BIGINT NOT NULL CHECK (sak_id > 0),
    status TEXT NOT NULL DEFAULT 'KLAR'
        CHECK (status IN ('KLAR', 'UNDER_ARBEID', 'FERDIG', 'FEILA', 'AVVENTER_AVKLARING')),
    steg TEXT NOT NULL DEFAULT 'GRUNNLAG'
        CHECK (steg IN ('GRUNNLAG', 'OPPRETT_BREV', 'SEND_BREV', 'FERDIG')),
    brev_id BIGINT,
    snapshot TEXT,
    antall_forsoek INTEGER NOT NULL DEFAULT 0,
    feilkode TEXT,
    laas_eier UUID,
    laas_til TIMESTAMPTZ,
    neste_forsoek TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (batch_id, sak_id)
);

CREATE INDEX brev_batch_sak_arbeid ON brev_batch_sak (neste_forsoek, laas_til)
    WHERE status IN ('KLAR', 'UNDER_ARBEID');
