create table trygdetid
(
    id                                                       uuid                                                              not null
        primary key,
    behandling_id                                            uuid                                                              not null,
    opprettet                                                timestamp with time zone default (now() AT TIME ZONE 'UTC'::text) not null,
    trygdetid_total                                          bigint,
    sak_id                                                   bigint,
    tidspunkt                                                timestamp,
    faktisk_trygdetid_norge_total                            text,
    faktisk_trygdetid_norge_antall_maaneder                  bigint,
    faktisk_trygdetid_teoretisk_total                        text,
    faktisk_trygdetid_teoretisk_antall_maaneder              bigint,
    fremtidig_trygdetid_norge_total                          text,
    fremtidig_trygdetid_norge_antall_maaneder                bigint,
    fremtidig_trygdetid_norge_opptjeningstid_maaneder        bigint,
    fremtidig_trygdetid_norge_mindre_enn_fire_femtedeler     boolean,
    fremtidig_trygdetid_teoretisk_total                      text,
    fremtidig_trygdetid_teoretisk_antall_maaneder            bigint,
    fremtidig_trygdetid_teoretisk_opptjeningstid_maaneder    bigint,
    fremtidig_trygdetid_teoretisk_mindre_enn_fire_femtedeler boolean,
    samlet_trygdetid_norge                                   bigint,
    samlet_trygdetid_teoretisk                               bigint,
    prorata_broek_teller                                     bigint,
    prorata_broek_nevner                                     bigint,
    trygdetid_tidspunkt                                      timestamp,
    trygdetid_regelresultat                                  text,
    beregnet_trygdetid_overstyrt                             boolean                  default false                            not null,
    poengaar_overstyrt                                       bigint,
    ident                                                    text                                                              not null,
    yrkesskade                                               boolean                  default false                            not null,
    beregnet_samlet_trygdetid_norge                          bigint,
    kopiert_grunnlag_fra_behandling                          uuid,
    overstyrt_begrunnelse                                    text,
    begrunnelse                                              text,
    unique (behandling_id, ident)
);

create table trygdetid_grunnlag
(
    id                     uuid not null
        primary key,
    trygdetid_id           uuid
        references trygdetid
            on delete cascade,
    type                   text not null,
    bosted                 text not null,
    periode_fra            date not null,
    periode_til            date not null,
    kilde                  text not null,
    trygdetid              integer,
    beregnet_verdi         text,
    beregnet_tidspunkt     timestamp with time zone,
    beregnet_regelresultat text,
    begrunnelse            text,
    poeng_inn_aar          boolean,
    poeng_ut_aar           boolean,
    prorata                boolean
);

create index trygdetid_grunnlag_trygdetid_id_idx
    on trygdetid_grunnlag (trygdetid_id);

create table opplysningsgrunnlag
(
    id           uuid not null
        primary key,
    trygdetid_id uuid
        references trygdetid
            on delete cascade,
    type         text not null,
    opplysning   jsonb,
    kilde        jsonb
);

create index opplysningsgrunnlag_trygdetid_id_idx
    on opplysningsgrunnlag (trygdetid_id);

create table trygdeavtale
(
    id                             uuid not null
        primary key,
    behandling_id                  uuid not null,
    avtale_kode                    text not null,
    avtale_dato_kode               text,
    avtale_kriteria_kode           text,
    kilde                          text,
    person_krets                   text,
    arb_inntekt                    text,
    arb_inntekt_kommentar          text,
    bereg_art                      text,
    bereg_art_kommentar            text,
    nordisk_trygdeavtale           text,
    nordisk_trygdeavtale_kommentar text
);
