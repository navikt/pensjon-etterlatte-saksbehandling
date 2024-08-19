package no.nav.etterlatte.brev

/*
Denne enumen brukes primært for kommunikasjonen mot brevbakeren.

Denne enumen henger tett sammen med Brevkoder-enumen.
Vurdér om du heller bør bruke den, hvis du er utenfor rein brevbaker-kontekst.
 */
enum class EtterlatteBrevKode(
    val brevtype: Brevtype,
    /*
    Bruk bare tittel-feltet her for vedlegg. Bruk ellers tittel-feltet i Brevkoder
    Har et mål om å få bort denne herifra, men da må vi først restrukturere litt så vedlegg får sin egen modell
     */
    val tittel: String? = null,
) {
    BARNEPENSJON_AVSLAG(Brevtype.VEDTAK),
    BARNEPENSJON_AVSLAG_UTFALL(Brevtype.VEDTAK),
    BARNEPENSJON_INNVILGELSE(Brevtype.VEDTAK),
    BARNEPENSJON_INNVILGELSE_UTFALL(Brevtype.VEDTAK),
    BARNEPENSJON_INNVILGELSE_FORELDRELOES(Brevtype.VEDTAK),
    BARNEPENSJON_INNVILGELSE_UTFALL_FORELDRELOES(Brevtype.VEDTAK),
    BARNEPENSJON_OPPHOER(Brevtype.VEDTAK),
    BARNEPENSJON_OPPHOER_UTFALL(Brevtype.VEDTAK),
    BARNEPENSJON_REVURDERING(Brevtype.VEDTAK),
    BARNEPENSJON_REVURDERING_UTFALL(Brevtype.VEDTAK),
    BARNEPENSJON_FORHAANDSVARSEL_OMREGNING(Brevtype.VEDTAK),
    BARNEPENSJON_VARSEL(Brevtype.VARSEL),
    BARNEPENSJON_VARSEL_UTFALL(Brevtype.VARSEL),
    BARNEPENSJON_VEDTAK_OMREGNING(Brevtype.VEDTAK),
    BARNEPENSJON_VEDTAK_OMREGNING_FERDIG(Brevtype.VEDTAK),
    BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL(Brevtype.VEDLEGG, "Trygdetid i vedlegg beregning av barnepensjon"),
    BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL(Brevtype.VEDLEGG, "Utfall ved forhåndsvarsel av feilutbetaling"),
    BARNEPENSJON_INFORMASJON_DOEDSFALL(Brevtype.INFORMASJON),
    BARNEPENSJON_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT(Brevtype.INFORMASJON),
    BARNEPENSJON_INFORMASJON_MOTTATT_SOEKNAD(Brevtype.INFORMASJON),
    BARNEPENSJON_INFORMASJON_INNHENTING_AV_OPPLYSNINGER(Brevtype.INFORMASJON),

    OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL(Brevtype.INFORMASJON),
    OMSTILLINGSSTOENAD_INFORMASJON_MOTTATT_SOEKNAD(Brevtype.INFORMASJON),
    OMSTILLINGSSTOENAD_INFORMASJON_INNHENTING_AV_OPPLYSNINGER(Brevtype.INFORMASJON),
    OMSTILLINGSSTOENAD_AVSLAG(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_AVSLAG_UTFALL(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_INNVILGELSE(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_OPPHOER(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_OPPHOER_UTFALL(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_REVURDERING(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_REVURDERING_UTFALL(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_VARSEL(Brevtype.VARSEL),
    OMSTILLINGSSTOENAD_VARSEL_UTFALL(Brevtype.VARSEL),
    OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT(Brevtype.VARSEL),
    OMSTILLINGSSTOENAD_VARSEL_AKTIVITETSPLIKT_UTFALL(Brevtype.VARSEL),
    OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL(Brevtype.VEDLEGG, "Utfall ved beregning av omstillingsstønad"),
    OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL(Brevtype.VEDLEGG, "Utfall ved forhåndsvarsel av feilutbetaling"),
    OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_VARSELBREV_INNHOLD(Brevtype.MANUELT, "Varsel om aktivitetsplikt for omstillingsstønad"),
    OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_4MND_INNHOLD(Brevtype.INFORMASJON),
    OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_INFORMASJON_6MND_INNHOLD(Brevtype.INFORMASJON),
    TILBAKEKREVING_INNHOLD(Brevtype.VEDTAK),
    TILBAKEKREVING_FERDIG(Brevtype.VEDTAK),

    AVVIST_KLAGE_INNHOLD(Brevtype.VEDTAK),
    AVVIST_KLAGE_FERDIG(Brevtype.VEDTAK),

    TOM_DELMAL(Brevtype.MANUELT),
    TOM_MAL_INFORMASJONSBREV(Brevtype.INFORMASJON),
    TOM_MAL(Brevtype.MANUELT),
    UTSATT_KLAGEFRIST(Brevtype.INFORMASJON),

    KLAGE_OVERSENDELSE_BRUKER(Brevtype.OVERSENDELSE_KLAGE),
    KLAGE_OVERSENDELSE_BLANKETT(Brevtype.NOTAT),

    OPPLASTET_PDF(Brevtype.OPPLASTET_PDF),
    INGEN_REDIGERBAR_DEL(Brevtype.INGEN),
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
