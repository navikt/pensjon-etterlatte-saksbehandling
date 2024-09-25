package no.nav.etterlatte.brev

/*
Breva våre er teknisk sett to brev som er satt sammen - den redigerbare delen, og delen som ligg fast.
Saksbehandler kan redigere den redigerbare delen, og ved PDF-generering sender vi den delen over til brevbakeren,
så den delen blir fletta inn i ferdigstillingsmalen.

For å få metadata og oppsett rundt riktig, er vi avhengige av å vite hvilken mal den redigerbare delen er basert på,
og hvilken mal ferdigstillingsdelen er basert på. Denne enumen er nettopp den koplinga.

Det er også ei logisk kopling mellom redigerbart utfall og ferdigstillingsmal - det vil for eksempel ikke gi meining
å bruke den redigerbare delen for tilbakekreving i ferdigstillingsmalen for vedtak avslag.
Så her modellerer vi det eksplisitt.
*/
enum class Brevkoder(
    val redigering: EtterlatteBrevKode,
    val ferdigstilling: EtterlatteBrevKode,
    val tittel: String,
    val brevtype: Brevtype,
) {
    OMREGNING(
        EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING,
        EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
        "Vedtak - endring av barnepensjon",
        Brevtype.VEDTAK,
    ),
    TILBAKEKREVING(
        EtterlatteBrevKode.TILBAKEKREVING_INNHOLD,
        EtterlatteBrevKode.TILBAKEKREVING_FERDIG,
        "Vedtak om tilbakekreving",
        Brevtype.VEDTAK,
    ),
    TOMT_INFORMASJONSBREV(
        EtterlatteBrevKode.TOM_DELMAL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjonsbrev",
        Brevtype.MANUELT,
    ),
    BP_AVSLAG(
        EtterlatteBrevKode.BARNEPENSJON_AVSLAG_UTFALL,
        EtterlatteBrevKode.BARNEPENSJON_AVSLAG,
        "Vedtak om avslått barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_INNVILGELSE(
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL,
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE,
        "Vedtak om innvilget barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_INNVILGELSE_FORELDRELOES(
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL_FORELDRELOES,
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_FORELDRELOES,
        "Vedtak om innvilget barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_OPPHOER(
        EtterlatteBrevKode.BARNEPENSJON_OPPHOER_UTFALL,
        EtterlatteBrevKode.BARNEPENSJON_OPPHOER,
        "Opphør av barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_REVURDERING(
        EtterlatteBrevKode.BARNEPENSJON_REVURDERING_UTFALL,
        EtterlatteBrevKode.BARNEPENSJON_REVURDERING,
        "Vedtak om endring av barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_VARSEL(
        EtterlatteBrevKode.BARNEPENSJON_VARSEL_UTFALL,
        EtterlatteBrevKode.BARNEPENSJON_VARSEL,
        "Forhåndsvarsel om ny barnepensjon fra 1. januar 2024",
        Brevtype.VARSEL,
    ),
    BP_INFORMASJON_DOEDSFALL(
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om barnepensjon",
        Brevtype.INFORMASJON,
    ),
    BP_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT(
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om barnepensjon",
        Brevtype.INFORMASJON,
    ),

    OMS_INFORMASJON_DOEDSFALL(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om omstillingsstønad",
        Brevtype.INFORMASJON,
    ),
    OMS_AVSLAG(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG,
        "Vedtak om avslått omstillingsstønad",
        Brevtype.VEDTAK,
    ),
    OMS_INNVILGELSE(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE,
        "Vedtak om innvilget omstillingsstønad",
        Brevtype.VEDTAK,
    ),
    OMS_OPPHOER(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER,
        "Opphør av omstillingsstønad",
        Brevtype.VEDTAK,
    ),
    OMS_REVURDERING(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING,
        "Vedtak om endring av omstillingsstønad",
        Brevtype.VEDTAK,
    ),
    OMS_INNTEKTSJUSTERING_VARSEL(
        EtterlatteBrevKode.INGEN_REDIGERBAR_DEL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNTEKTSJUSTERING_VARSEL,
        "Varselbrev - oppdatere endringer i inntekt",
        Brevtype.VARSEL,
    ),
    OMS_INNTEKTSJUSTERING_VEDTAK(
        EtterlatteBrevKode.INGEN_REDIGERBAR_DEL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNTEKTSJUSTERING_VEDTAK,
        "Vedtak om inntektjsutering",
        Brevtype.VEDTAK,
    ),
    OMS_VARSEL(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL,
        "Varsel - omstillingsstønad",
        Brevtype.VARSEL,
    ),
    OMS_VARSEL_AKTIVITETSPLIKT(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT,
        "Varselbrev - stans om ikke akt.plikt oppfylt",
        Brevtype.VARSEL,
    ),

    UTSATT_KLAGEFRIST_INFORMASJONSBREV(
        EtterlatteBrevKode.UTSATT_KLAGEFRIST,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om barnepensjon fra 1. januar 2024",
        Brevtype.INFORMASJON,
    ),
    AVVIST_KLAGE(
        EtterlatteBrevKode.AVVIST_KLAGE_INNHOLD,
        EtterlatteBrevKode.AVVIST_KLAGE_FERDIG,
        "Vedtak om avvist klage",
        Brevtype.VEDTAK,
    ),

    OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om aktivitetsplikt for omstillingsstønad",
        Brevtype.INFORMASJON,
    ),
    OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om aktivitetsplikt for omstillingsstønad",
        Brevtype.INFORMASJON,
    ),
    OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Vi har mottatt søknaden din om omstillingsstønad",
        Brevtype.INFORMASJON,
    ),
    OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Du må sende oss flere opplysninger",
        Brevtype.INFORMASJON,
    ),
    BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD(
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Vi har mottatt søknaden din om barnepensjon",
        Brevtype.INFORMASJON,
    ),
    BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER(
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Du må sende oss flere opplysninger",
        Brevtype.INFORMASJON,
    ),

    OVERSENDELSE_KLAGE(
        EtterlatteBrevKode.INGEN_REDIGERBAR_DEL,
        EtterlatteBrevKode.KLAGE_OVERSENDELSE_BRUKER,
        tittel = "Klagen er oversendt til NAV Klageinstans Vest",
        Brevtype.OVERSENDELSE_KLAGE,
    ),
    KLAGE_OVERSENDELSE_BLANKETT(
        EtterlatteBrevKode.INGEN_REDIGERBAR_DEL,
        EtterlatteBrevKode.KLAGE_OVERSENDELSE_BLANKETT,
        "Oversendelse til KA",
        Brevtype.NOTAT,
    ),

    OPPLASTET_PDF(
        EtterlatteBrevKode.INGEN_REDIGERBAR_DEL,
        EtterlatteBrevKode.OPPLASTET_PDF,
        "Ubrukt tittel",
        Brevtype.OPPLASTET_PDF,
    ),
    ;

    init {
        require(redigering != ferdigstilling) {
            "Bruk forskjellige maler for redigering og ferdigstilling. $redigering og $ferdigstilling er like"
        }
    }
}
