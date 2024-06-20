package no.nav.etterlatte.brev

enum class Brevkoder(
    val redigering: EtterlatteBrevKode,
    val ferdigstilling: EtterlatteBrevKode = redigering,
) {
    OMREGNING(
        EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING,
        EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
    ),
    TILBAKEKREVING(
        EtterlatteBrevKode.TILBAKEKREVING_INNHOLD,
        EtterlatteBrevKode.TILBAKEKREVING_FERDIG,
    ),
    TOMT_INFORMASJONSBREV(
        EtterlatteBrevKode.TOM_DELMAL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
    ),
    BP_AVSLAG(
        EtterlatteBrevKode.BARNEPENSJON_AVSLAG_UTFALL,
        EtterlatteBrevKode.BARNEPENSJON_AVSLAG,
    ),
    BP_INNVILGELSE(
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL,
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE,
    ),
    BP_INNVILGELSE_FORELDRELOES(
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL_FORELDRELOES,
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_FORELDRELOES,
    ),
    BP_OPPHOER(EtterlatteBrevKode.BARNEPENSJON_OPPHOER_UTFALL, EtterlatteBrevKode.BARNEPENSJON_OPPHOER),
    BP_REVURDERING(EtterlatteBrevKode.BARNEPENSJON_REVURDERING_UTFALL, EtterlatteBrevKode.BARNEPENSJON_REVURDERING),
    BP_VARSEL(EtterlatteBrevKode.BARNEPENSJON_VARSEL_UTFALL, EtterlatteBrevKode.BARNEPENSJON_VARSEL),
    BP_INFORMASJON_DOEDSFALL(
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
    ),

    OMS_INFORMASJON_DOEDSFALL(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
    ),
    OMS_AVSLAG(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG,
    ),
    OMS_INNVILGELSE(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE,
    ),
    OMS_OPPHOER(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER,
    ),
    OMS_REVURDERING(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING,
    ),
    OMS_VARSEL(EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_UTFALL, EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL),
    OMS_VARSEL_AKTIVITETSPLIKT(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT,
    ),

    UTSATT_KLAGEFRIST_INFORMASJONSBREV(
        EtterlatteBrevKode.UTSATT_KLAGEFRIST,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
    ),
    AVVIST_KLAGE(EtterlatteBrevKode.AVVIST_KLAGE_INNHOLD, EtterlatteBrevKode.AVVIST_KLAGE_FERDIG),

    OVERSENDELSE_KLAGE(EtterlatteBrevKode.KLAGE_OVERSENDELSE_BRUKER),
}
