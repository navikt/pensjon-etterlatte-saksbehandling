package no.nav.etterlatte.brev

interface Brevbakerkode

/*
Denne enumen brukes primært for kommunikasjonen mot brevbakeren.

Denne enumen henger tett sammen med Brevkoder-enumen.
Vurdér om du heller bør bruke den, hvis du er utenfor rein brevbaker-kontekst.
 */
enum class EtterlatteBrevKode : Brevbakerkode {
    BARNEPENSJON_AVSLAG,
    BARNEPENSJON_AVSLAG_UTFALL,
    BARNEPENSJON_INNVILGELSE,
    BARNEPENSJON_INNVILGELSE_UTFALL,
    BARNEPENSJON_INNVILGELSE_FORELDRELOES,
    BARNEPENSJON_INNVILGELSE_UTFALL_FORELDRELOES,
    BARNEPENSJON_OPPHOER,
    BARNEPENSJON_OPPHOER_UTFALL,
    BARNEPENSJON_REVURDERING,
    BARNEPENSJON_REVURDERING_UTFALL,
    BARNEPENSJON_FORHAANDSVARSEL_OMREGNING,
    BARNEPENSJON_VARSEL,
    BARNEPENSJON_VARSEL_UTFALL,
    BARNEPENSJON_VEDTAK_OMREGNING,
    BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
    BARNEPENSJON_INFORMASJON_DOEDSFALL,
    BARNEPENSJON_18_20_VED_REFORMSTIDSPUNKT,
    BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD,
    BARNEPENSJON_INNHENTING_AV_OPPLYSNINGER,

    OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL,
    OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD,
    OMSTILLINGSSTOENAD_INNHENTING_AV_OPPLYSNINGER,
    OMSTILLINGSSTOENAD_AVSLAG,
    OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
    OMSTILLINGSSTOENAD_INNVILGELSE,
    OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
    OMSTILLINGSSTOENAD_OPPHOER,
    OMSTILLINGSSTOENAD_OPPHOER_UTFALL,
    OMSTILLINGSSTOENAD_REVURDERING,
    OMSTILLINGSSTOENAD_REVURDERING_UTFALL,
    OMSTILLINGSSTOENAD_VARSEL,
    OMSTILLINGSSTOENAD_VARSEL_UTFALL,
    OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT,
    OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT_UTFALL,
    AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD,
    AKTIVITETSPLIKT_INFORMASJON_10MND_INNHOLD,
    AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD,
    OMSTILLINGSSTOENAD_INNTEKTSJUSTERING_VARSEL,
    OMSTILLINGSSTOENAD_INNTEKTSJUSTERING_VEDTAK,
    OMSTILLINGSSTOENAD_INNTEKTSJUSTERING_VEDTAK_UTFALL,

    TILBAKEKREVING_INNHOLD,
    TILBAKEKREVING_FERDIG,

    AVVIST_KLAGE_INNHOLD,
    AVVIST_KLAGE_FERDIG,
    KLAGE_SAKSBEHANDLINGS_INFO,
    TOM_DELMAL,
    TOM_MAL_INFORMASJONSBREV,
    UTSATT_KLAGEFRIST,

    KLAGE_OVERSENDELSE_BRUKER,
    KLAGE_OVERSENDELSE_BLANKETT,

    OPPLASTET_PDF,
    INGEN_REDIGERBAR_DEL,

    OMS_EO_VARSEL_TILBAKEKREVING,
    OMS_EO_VARSEL_TILBAKEKREVING_INNHOLD,

    OMS_EO_FORHAANDSVARSEL,
    OMS_EO_FORHAANDSVARSEL_INNHOLD,

    OMS_EO_VEDTAK,
    OMS_EO_VEDTAK_UTFALL,
}

enum class Brevtype {
    VEDTAK,
    VARSEL,
    INFORMASJON,
    OPPLASTET_PDF,
    MANUELT,
    VEDLEGG,
    NOTAT,
    OVERSENDELSE_KLAGE,
    SLETTET_VARSEL,
    INGEN, // Til bruk for å modellere at brevet ikke har en redigerbar del
    ;

    fun erKobletTilEnBehandling(): Boolean = this in listOf(VEDTAK, VARSEL, VEDLEGG)
}

enum class Vedlegg(
    val tittel: String,
) : Brevbakerkode {
    BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL("Trygdetid i vedlegg beregning av barnepensjon"),
    BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL("Utfall ved forhåndsvarsel av feilutbetaling"),
    OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL("Utfall ved beregning av omstillingsstønad"),
    OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL("Utfall ved forhåndsvarsel av feilutbetaling"),
    OMS_EO_FORHAANDSVARSEL_BEREGNINGVEDLEGG_INNHOLD("Utfall etteroppgjør beregningvedlegg"),
}
