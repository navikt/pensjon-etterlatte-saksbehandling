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
    val ferdigstilling: EtterlatteBrevKode = redigering,
    val tittel: String? = null,
) {
    OMREGNING(
        EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING,
        EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
        "Vedtak - endring av barnepensjon",
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
        "Vedtak om avslått barnepensjon",
    ),
    BP_INNVILGELSE(
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL,
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE,
        "Vedtak om innvilget barnepensjon",
    ),
    BP_INNVILGELSE_FORELDRELOES(
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL_FORELDRELOES,
        EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_FORELDRELOES,
        "Vedtak om innvilget barnepensjon",
    ),
    BP_OPPHOER(EtterlatteBrevKode.BARNEPENSJON_OPPHOER_UTFALL, EtterlatteBrevKode.BARNEPENSJON_OPPHOER, "Opphør av barnepensjon"),
    BP_REVURDERING(EtterlatteBrevKode.BARNEPENSJON_REVURDERING_UTFALL, EtterlatteBrevKode.BARNEPENSJON_REVURDERING),
    BP_VARSEL(
        EtterlatteBrevKode.BARNEPENSJON_VARSEL_UTFALL,
        EtterlatteBrevKode.BARNEPENSJON_VARSEL,
        "Forhåndsvarsel om ny barnepensjon fra 1. januar 2024",
    ),
    BP_INFORMASJON_DOEDSFALL(
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om barnepensjon",
    ),
    BP_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT(
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om barnepensjon",
    ),

    OMS_INFORMASJON_DOEDSFALL(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om omstillingsstønad",
    ),
    OMS_AVSLAG(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG,
        "Vedtak om avslått omstillingsstønad",
    ),
    OMS_INNVILGELSE(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE,
        "Vedtak om innvilget omstillingsstønad",
    ),
    OMS_OPPHOER(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER,
        "Opphør av omstillingsstønad",
    ),
    OMS_REVURDERING(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING,
    ),
    OMS_VARSEL(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL,
        "Varsel - omstillingsstønad",
    ),
    OMS_VARSEL_AKTIVITETSPLIKT(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT_UTFALL,
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT,
        "Varselbrev - stans om ikke akt.plikt oppfylt",
    ),

    UTSATT_KLAGEFRIST_INFORMASJONSBREV(
        EtterlatteBrevKode.UTSATT_KLAGEFRIST,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om barnepensjon fra 1. januar 2024",
    ),
    AVVIST_KLAGE(EtterlatteBrevKode.AVVIST_KLAGE_INNHOLD, EtterlatteBrevKode.AVVIST_KLAGE_FERDIG),

    OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om aktivitetsplikt for omstillingsstønad",
    ),
    OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om aktivitetsplikt for omstillingsstønad",
    ),
    OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Vi har mottatt søknaden din om omstillingsstønad",
    ),
    OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER(
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Du må sende oss flere opplysninger",
    ),
    OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL( // TODO: Denne er duplikat og skal smelte saman med OMS_INFORMASJON_DOEDSFALL
        EtterlatteBrevKode.OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om omstillingsstønad",
    ),
    BARNEPENSJON_INFORMASJON_DOEDSFALL( // TODO: Denne er duplikat og skal smelte saman med BP_INFORMASJON_DOEDSFALL
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om barnepensjon",
    ),
    BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD(
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Vi har mottatt søknaden din om barnepensjon",
    ),
    BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER(
        EtterlatteBrevKode.BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Du må sende oss flere opplysninger",
    ),

    OVERSENDELSE_KLAGE(EtterlatteBrevKode.KLAGE_OVERSENDELSE_BRUKER, tittel = "Klagen er oversendt til NAV Klageinstans Vest"),
    KLAGE_OVERSENDELSE_BLANKETT(EtterlatteBrevKode.KLAGE_OVERSENDELSE_BLANKETT, tittel = "Oversendelse til KA"),

    OPPLASTET_PDF(EtterlatteBrevKode.OPPLASTET_PDF),
}
