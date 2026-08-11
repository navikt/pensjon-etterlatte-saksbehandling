create table public.beregningsperiode
(
    id                         uuid not null
        primary key,
    beregningid                uuid not null,
    behandlingid               uuid not null,
    beregnetdato               timestamp,
    datofom                    text,
    datotom                    text,
    utbetaltbeloep             bigint,
    soeskenflokk               jsonb,
    grunnbeloepmnd             bigint,
    grunnbeloep                bigint,
    sakid                      bigint,
    grunnlagversjon            bigint,
    trygdetid                  bigint,
    regelresultat              jsonb,
    regelversjon               text,
    type                       text,
    kilde                      text,
    institusjonsopphold        jsonb,
    beregnings_metode          text,
    samlet_norsk_trygdetid     bigint,
    samlet_teoretisk_trygdetid bigint,
    prorata_broek_nevner       bigint,
    prorata_broek_teller       bigint,
    trygdetid_for_ident        text,
    avdoede_foreldre           jsonb,
    kun_en_juridisk_forelder   boolean,
    regelverk                  text not null,
    har_foreldreloessats       boolean
);

alter table public.beregningsperiode
    owner to "etterlatte-beregning";

create index index_name
    on public.beregningsperiode (behandlingid);

create table public.avkortingsgrunnlag_forventet
(
    id                                      uuid not null
        constraint avkortinggrunnlag_pkey
            primary key,
    behandling_id                           uuid not null,
    fom                                     date,
    tom                                     date,
    aarsinntekt                             bigint,
    spesifikasjon                           text,
    kilde                                   text,
    fratrekk_inn_ut                         text,
    relevante_maaneder                      text,
    inntekt_utland                          bigint,
    fratrekk_inn_aar_utland                 bigint,
    aarsoppgjoer_id                         uuid not null,
    overstyrt_innvilga_maaneder_aarsak      text,
    overstyrt_innvilga_maaneder_begrunnelse text,
    inntekt_tom                             bigint,
    inntekt_utland_tom                      bigint
);

alter table public.avkortingsgrunnlag_forventet
    owner to "etterlatte-beregning";

create table public.avkortingsperioder
(
    id               uuid not null
        constraint beregnet_avkortinggrunnlag_pkey
            primary key,
    behandling_id    uuid not null,
    fom              date,
    tom              date,
    avkorting        bigint,
    tidspunkt        timestamp,
    regel_resultat   text,
    kilde            text,
    inntektsgrunnlag uuid,
    aarsoppgjoer_id  uuid not null
);

alter table public.avkortingsperioder
    owner to "etterlatte-beregning";

create table public.beregningsgrunnlag
(
    behandlings_id                   uuid not null
        constraint bp_beregningsgrunnlag_pkey
            primary key,
    soesken_med_i_beregning          text,
    kilde                            text,
    institusjonsopphold              text,
    soesken_med_i_beregning_perioder jsonb,
    beregningsmetode                 text,
    beregnings_metode_flere_avdoede  jsonb,
    kun_en_juridisk_forelder         jsonb
);

alter table public.beregningsgrunnlag
    owner to "etterlatte-beregning";

create table public.avkortet_ytelse
(
    id                                   uuid not null
        primary key,
    behandling_id                        uuid not null,
    fom                                  date,
    tom                                  date,
    ytelse_etter_avkorting               bigint,
    tidspunkt                            timestamp,
    regel_resultat                       text,
    kilde                                text,
    avkortingsbeloep                     bigint,
    ytelse_foer_avkorting                bigint,
    ytelse_etter_avkorting_uten_restanse bigint,
    type                                 text,
    inntektsgrunnlag                     uuid,
    restanse                             uuid,
    aarsoppgjoer_id                      uuid not null,
    sanksjon_id                          uuid
);

alter table public.avkortet_ytelse
    owner to "etterlatte-beregning";

create table public.avkorting_aarsoppgjoer_ytelse_foer_avkorting
(
    id                  uuid   not null,
    behandling_id       uuid   not null,
    beregning           bigint not null,
    fom                 date   not null,
    tom                 date,
    beregningsreferanse uuid,
    aarsoppgjoer_id     uuid   not null
);

alter table public.avkorting_aarsoppgjoer_ytelse_foer_avkorting
    owner to "etterlatte-beregning";

create table public.avkorting_aarsoppgjoer_restanse
(
    id               uuid   not null,
    behandling_id    uuid   not null,
    total_restanse   bigint not null,
    fordelt_restanse bigint not null,
    tidspunkt        timestamp,
    regel_resultat   text,
    kilde            text,
    aarsoppgjoer_id  uuid   not null
);

alter table public.avkorting_aarsoppgjoer_restanse
    owner to "etterlatte-beregning";

create table public.overstyr_beregning
(
    sak_id      bigint                          not null
        primary key,
    beskrivelse text                            not null,
    tidspunkt   timestamp default now(),
    status      text      default 'AKTIV'::text not null,
    kategori    text
);

alter table public.overstyr_beregning
    owner to "etterlatte-beregning";

create table public.overstyr_beregningsgrunnlag
(
    id                       uuid                  not null
        primary key,
    behandlings_id           uuid                  not null,
    dato_fra_og_med          date,
    dato_til_og_med          date,
    utbetalt_beloep          bigint,
    trygdetid                bigint,
    sak_id                   bigint,
    beskrivelse              text default ''::text not null,
    kilde                    text,
    prorata_broek_teller     bigint,
    prorata_broek_nevner     bigint,
    trygdetid_for_ident      text,
    regulering_regelresultat text,
    aarsak                   text,
    har_foreldreloessats     boolean
);

alter table public.overstyr_beregningsgrunnlag
    owner to "etterlatte-beregning";

create table public.sanksjon
(
    id            uuid                       not null
        primary key,
    behandling_id uuid                       not null,
    sak_id        bigint                     not null,
    fom           date,
    tom           date,
    opprettet     text,
    endret        text,
    beskrivelse   text,
    sanksjon_type text default 'STANS'::text not null
);

alter table public.sanksjon
    owner to "etterlatte-beregning";

create table public.avkorting_aarsoppgjoer
(
    id                uuid                  not null
        primary key,
    behandling_id     uuid                  not null,
    sak_id            bigint                not null,
    aar               smallint              not null,
    innvilga_maaneder text,
    fom               date                  not null,
    er_etteroppgjoer  boolean default false not null,
    unique (behandling_id, aar)
);

alter table public.avkorting_aarsoppgjoer
    owner to "etterlatte-beregning";

create table public.anvendt_trygdetid
(
    id                uuid default gen_random_uuid() not null
        primary key,
    behandling_id     uuid                           not null,
    foer_kombinering  jsonb                          not null,
    etter_kombinering jsonb                          not null
);

alter table public.anvendt_trygdetid
    owner to "etterlatte-beregning";

create table public.inntekt_innvilget
(
    id             uuid default gen_random_uuid() not null
        primary key,
    grunnlag_id    uuid                           not null,
    behandling_id  uuid                           not null,
    inntekt        bigint                         not null,
    regel_resultat text                           not null,
    kilde          text                           not null,
    tidspunkt      timestamp                      not null
);

alter table public.inntekt_innvilget
    owner to "etterlatte-beregning";

create table public.avkortingsgrunnlag_faktisk
(
    id                  uuid default gen_random_uuid() not null
        primary key,
    behandling_id       uuid                           not null,
    aarsoppgjoer_id     uuid                           not null,
    fom                 date                           not null,
    tom                 date                           not null,
    innvilgede_maaneder integer                        not null,
    loennsinntekt       bigint                         not null,
    naeringsinntekt     bigint                         not null,
    afp                 bigint                         not null,
    utlandsinntekt      bigint                         not null,
    kilde               text                           not null,
    spesifikasjon       text
);

alter table public.avkortingsgrunnlag_faktisk
    owner to "etterlatte-beregning";

create table public.etteroppgjoer_beregnet_resultat
(
    id                                  uuid      not null
        primary key,
    aar                                 integer   not null,
    siste_iverksatte_behandling_id      uuid      not null,
    forbehandling_id                    uuid      not null,
    utbetalt_stoenad                    bigint    not null,
    ny_brutto_stoenad                   bigint    not null,
    differanse                          bigint    not null,
    grense                              text      not null,
    resultat_type                       text      not null,
    tidspunkt                           timestamp not null,
    regel_resultat                      text      not null,
    kilde                               text      not null,
    referanse_avkorting_sist_iverksatte uuid      not null,
    referanse_avkorting_forbehandling   uuid      not null,
    har_ingen_inntekt                   boolean,
    constraint etteroppgjoer_beregnet_result_aar_siste_iverksatte_behandli_key
        unique (aar, siste_iverksatte_behandling_id, forbehandling_id)
);

alter table public.etteroppgjoer_beregnet_resultat
    owner to "etterlatte-beregning";
