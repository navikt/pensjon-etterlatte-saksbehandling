create table vedtaksperioder
(
    id uuid primary key default gen_random_uuid(),
    behandling_id uuid not null,
    fra_og_med    text not null,
    til_og_med    text
);