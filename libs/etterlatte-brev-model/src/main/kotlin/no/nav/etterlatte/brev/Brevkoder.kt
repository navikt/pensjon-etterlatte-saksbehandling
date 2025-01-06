package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.feilhaandtering.krev

/*
Breva våre er teknisk sett to brev som er satt sammen - den redigerbare delen, og delen som ligg fast.
Saksbehandler kan redigere den redigerbare delen, og ved PDF-generering sender vi den delen over til brevbakeren,
så den delen blir fletta inn i ferdigstillingsmalen.

For å få metadata og oppsett rundt riktig, er vi avhengige av å vite hvilken mal den redigerbare delen er basert på,
og hvilken mal ferdigstillingsdelen er basert på. Denne enumen er nettopp den koplinga.

Det er også ei logisk kopling mellom redigerbart utfall og ferdigstillingsmal - det vil for eksempel ikke gi meining
å bruke den redigerbare delen for tilbakekreving i ferdigstillingsmalen for vedtak avslag.
Så her modellerer vi det eksplisitt.

BP = Barnepensjon
OMS = Omstillingsstoenad
*/
enum class Brevkoder(
    val redigering: EtterlatteBrevKode,
    val ferdigstilling: EtterlatteBrevKode,
    val tittel: String,
    val brevtype: Brevtype,
    val titlerPaaSpraak: Map<Spraak, String> = emptyMap(),
) {
    OMREGNING(
        EtterlatteBrevKode.BP_VEDTAK_OMREGNING,
        EtterlatteBrevKode.BP_VEDTAK_OMREGNING_FERDIG,
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
        EtterlatteBrevKode.BP_AVSLAG_UTFALL,
        EtterlatteBrevKode.BP_AVSLAG,
        "Vedtak om avslått barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_INNVILGELSE(
        EtterlatteBrevKode.BP_INNVILGELSE_UTFALL,
        EtterlatteBrevKode.BP_INNVILGELSE,
        "Vedtak om innvilget barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_INNVILGELSE_FORELDRELOES(
        EtterlatteBrevKode.BP_INNVILGELSE_UTFALL_FORELDRELOES,
        EtterlatteBrevKode.BP_INNVILGELSE_FORELDRELOES,
        "Vedtak om innvilget barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_OPPHOER(
        EtterlatteBrevKode.BP_OPPHOER_UTFALL,
        EtterlatteBrevKode.BP_OPPHOER,
        "Opphør av barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_REVURDERING(
        EtterlatteBrevKode.BP_REVURDERING_UTFALL,
        EtterlatteBrevKode.BP_REVURDERING,
        "Vedtak om endring av barnepensjon",
        Brevtype.VEDTAK,
    ),
    BP_VARSEL(
        EtterlatteBrevKode.BP_VARSEL_UTFALL,
        EtterlatteBrevKode.BP_VARSEL,
        "Forhåndsvarsel om ny barnepensjon fra 1. januar 2024",
        Brevtype.VARSEL,
    ),
    BP_INFORMASJON_DOEDSFALL(
        EtterlatteBrevKode.BP_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om barnepensjon",
        Brevtype.INFORMASJON,
    ),
    BP_DOEDSFALL_18_20_VED_REFORMTIDSPUNKT(
        EtterlatteBrevKode.BP_DOEDSFALL_18_20_VED_REFORMTIDSPUNKT,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om barnepensjon",
        Brevtype.INFORMASJON,
    ),

    OMS_INFORMASJON_DOEDSFALL(
        EtterlatteBrevKode.OMS_INFORMASJON_DOEDSFALL,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om omstillingsstønad",
        Brevtype.INFORMASJON,
    ),
    OMS_AVSLAG(
        EtterlatteBrevKode.OMS_AVSLAG_UTFALL,
        EtterlatteBrevKode.OMS_AVSLAG,
        "Vedtak om avslått omstillingsstønad",
        Brevtype.VEDTAK,
    ),
    OMS_INNVILGELSE(
        EtterlatteBrevKode.OMS_INNVILGELSE_UTFALL,
        EtterlatteBrevKode.OMS_INNVILGELSE,
        "Vedtak om innvilget omstillingsstønad",
        Brevtype.VEDTAK,
    ),
    OMS_OPPHOER(
        EtterlatteBrevKode.OMS_OPPHOER_UTFALL,
        EtterlatteBrevKode.OMS_OPPHOER,
        "Opphør av omstillingsstønad",
        Brevtype.VEDTAK,
    ),
    OMS_REVURDERING(
        EtterlatteBrevKode.OMS_REVURDERING_UTFALL,
        EtterlatteBrevKode.OMS_REVURDERING,
        "Vedtak om endring av omstillingsstønad",
        Brevtype.VEDTAK,
    ),
    OMS_INNTEKTSJUSTERING_VEDTAK(
        EtterlatteBrevKode.OMS_INNTEKTSJUSTERING_VEDTAK_UTFALL,
        EtterlatteBrevKode.OMS_INNTEKTSJUSTERING_VEDTAK,
        "Varsel og utkast til vedtak inntekt nytt år",
        Brevtype.VEDTAK,
    ),

    OMS_VARSEL(
        EtterlatteBrevKode.OMS_VARSEL_UTFALL,
        EtterlatteBrevKode.OMS_VARSEL,
        "Varsel - omstillingsstønad",
        Brevtype.VARSEL,
    ),
    OMS_VARSEL_AKTIVITETSPLIKT(
        EtterlatteBrevKode.OMS_VARSEL_AKTIVITETSPLIKT_UTFALL,
        EtterlatteBrevKode.OMS_VARSEL_AKTIVITETSPLIKT,
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
    KLAGE_SAKSBEHANDLINGSTID(
        EtterlatteBrevKode.KLAGE_SAKSBEHANDLINGS_INFO,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Klage – orientering om saksbehandlingstid",
        Brevtype.INFORMASJON,
        titlerPaaSpraak =
            mapOf(
                Spraak.NB to "Klage – orientering om saksbehandlingstid",
                Spraak.NN to "Klage – orientering om saksbehandlingstid",
                Spraak.EN to "Appeals - Information about processing time",
            ),
    ),
    OMS_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD(
        EtterlatteBrevKode.OMS_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om aktivitetsplikt for omstillingsstønad",
        Brevtype.INFORMASJON,
        titlerPaaSpraak =
            mapOf(
                Spraak.NB to "Informasjon om aktivitetsplikt for omstillingsstønad",
                Spraak.NN to "Informasjon om aktivitetsplikt for omstillingsstønad",
                Spraak.EN to "Information about the activity requirement for adjustment allowance",
            ),
    ),
    OMS_AKTIVITETSPLIKT_INFORMASJON_10MND_INNHOLD(
        EtterlatteBrevKode.OMS_AKTIVITETSPLIKT_INFORMASJON_10MND_INNHOLD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om aktivitetsplikt for omstillingsstønad",
        Brevtype.INFORMASJON,
        titlerPaaSpraak =
            mapOf(
                Spraak.NB to "Informasjon om aktivitetsplikt for omstillingsstønad",
                Spraak.NN to "Informasjon om aktivitetsplikt for omstillingsstønad",
                Spraak.EN to "Information about the activity requirement for adjustment allowance",
            ),
    ),
    OMS_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD(
        EtterlatteBrevKode.OMS_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Informasjon om omstillingsstønaden din",
        Brevtype.INFORMASJON,
    ),
    OMS_INFORMASJON_MOTTATT_SOEKNAD(
        EtterlatteBrevKode.OMS_INFORMASJON_MOTTATT_SOEKNAD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Vi har mottatt søknaden din om omstillingsstønad",
        Brevtype.INFORMASJON,
    ),
    OMS_INFORMASJON_INNHENTING_AV_OPPLYSNINGER(
        EtterlatteBrevKode.OMS_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Du må sende oss flere opplysninger",
        Brevtype.INFORMASJON,
    ),
    BP_INFORMASJON_MOTTATT_SOEKNAD(
        EtterlatteBrevKode.BP_INFORMASJON_MOTTATT_SOEKNAD,
        EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
        "Vi har mottatt søknaden din om barnepensjon",
        Brevtype.INFORMASJON,
    ),
    BP_INFORMASJON_INNHENTING_AV_OPPLYSNINGER(
        EtterlatteBrevKode.BP_INFORMASJON_INNHENTING_AV_OPPLYSNINGER,
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
        krev(redigering != ferdigstilling) {
            "Bruk forskjellige maler for redigering og ferdigstilling. $redigering og $ferdigstilling er like"
        }
    }
}
