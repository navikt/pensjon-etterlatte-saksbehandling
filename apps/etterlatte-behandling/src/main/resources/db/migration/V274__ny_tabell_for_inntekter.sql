create table etteroppgjoer_summerte_inntekter
(
    id                 uuid primary key default gen_random_uuid(),
    forbehandling_id   uuid references etteroppgjoer_behandling (id) not null,
    behandling_id      uuid                                          not null,
    afp                jsonb                                         not null,
    loenn              jsonb                                         not null,
    oms                jsonb                                         not null,
    tidspunkt_beregnet timestamp                                     not null,
    regel_resultat     jsonb                                         not null,
    unique (forbehandling_id, behandling_id)
);


