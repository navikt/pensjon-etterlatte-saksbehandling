create table behandling_info
(
    behandling_id UUID PRIMARY KEY
        CONSTRAINT behandling_info_behandling_fk_id REFERENCES behandling (id),
    oppdatert     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    etterbetaling JSONB,
    brevutfall    JSONB
)