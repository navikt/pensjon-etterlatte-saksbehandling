create table revurdering_info (
    behandling_id uuid PRIMARY KEY references behandling (id),
    kilde JSONB,
    info JSONB
)