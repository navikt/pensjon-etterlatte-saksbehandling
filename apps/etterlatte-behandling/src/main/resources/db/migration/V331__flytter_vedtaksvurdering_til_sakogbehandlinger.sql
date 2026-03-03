create table public.vedtak
(
    id                   bigserial
        primary key,
    sakid                bigint,
    behandlingid         uuid not null,
    saksbehandlerid      varchar,
    vilkaarsresultat     text,
    beregningsresultat   text,
    fnr                  text,
    datofattet           text,
    datoattestert        text,
    attestant            text,
    datovirkfom          date,
    vedtakstatus         text,
    saktype              text,
    behandlingtype       text,
    fattetvedtakenhet    char(4),
    attestertvedtakenhet char(4),
    type                 varchar,
    avkorting            text,
    revurderingsaarsak   text,
    revurderinginfo      text,
    tilbakekreving       text,
    klage                text,
    opphoer_fom          date,
    datoiverksatt        timestamp with time zone
);

create unique index idx_sakid_behandlingid
    on public.vedtak (sakid, behandlingid);

create unique index behandlingid
    on public.vedtak (behandlingid);

create index idx_fnr
    on public.vedtak (fnr);

create table public.utbetalingsperiode
(
    id        bigserial
        primary key,
    vedtakid  bigint not null,
    datofom   date   not null,
    datotom   date,
    type      text   not null,
    beloep    numeric,
    regelverk text   not null
);

create index idx_vedtakid
    on public.utbetalingsperiode (vedtakid);

create table public.outbox_vedtakshendelse
(
    id        uuid                     default gen_random_uuid()               not null
        primary key,
    vedtakid  bigint                                                            not null
        references public.vedtak,
    type      text                                                              not null,
    opprettet timestamp with time zone default (now() AT TIME ZONE 'UTC'::text) not null,
    publisert boolean                  default false
);

create index outbox_vedtakshendelse_publisert_idx
    on public.outbox_vedtakshendelse (publisert);

create table public.avkortet_ytelse_periode
(
    id          uuid                     default gen_random_uuid()               not null
        primary key,
    vedtakid    bigint                                                            not null
        references public.vedtak,
    opprettet   timestamp with time zone default (now() AT TIME ZONE 'UTC'::text) not null,
    datofom     date                                                              not null,
    datotom     date,
    type        text                                                              not null,
    ytelsefoer  integer                                                           not null,
    ytelseetter integer                                                           not null
);

create index avkortet_ytelse_periode_vedtakid_idx
    on public.avkortet_ytelse_periode (vedtakid);

create table public.samordning_manuell
(
    id            uuid                     default gen_random_uuid()               not null
        primary key,
    opprettet     timestamp with time zone default (now() AT TIME ZONE 'UTC'::text) not null,
    opprettet_av  text                                                              not null,
    vedtakid      bigint                                                            not null
        references public.vedtak,
    samid         bigint                                                            not null,
    refusjonskrav boolean                                                           not null,
    kommentar     text                                                              not null
);

create index samordning_manuell_vedtakid_idx on public.samordning_manuell (vedtakid);