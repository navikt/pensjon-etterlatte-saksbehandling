package no.nav.etterlatte.brev

enum class EtterlatteBrevKode(val brevtype: Brevtype, val tittel: String? = null) {
    BARNEPENSJON_AVSLAG(Brevtype.VEDTAK, "Vedtak om avslått barnepensjon"),
    BARNEPENSJON_AVSLAG_UTFALL(Brevtype.VEDTAK, "Vedtak om avslått barnepensjon"),
    BARNEPENSJON_INNVILGELSE(Brevtype.VEDTAK, "Vedtak om innvilget barnepensjon"),
    BARNEPENSJON_INNVILGELSE_UTFALL(Brevtype.VEDTAK, "Vedtak om innvilget barnepensjon"),
    BARNEPENSJON_INNVILGELSE_FORELDRELOES(Brevtype.VEDTAK, "Vedtak om innvilget barnepensjon"),
    BARNEPENSJON_INNVILGELSE_UTFALL_FORELDRELOES(Brevtype.VEDTAK, "Vedtak om innvilget barnepensjon"),
    BARNEPENSJON_OPPHOER(Brevtype.VEDTAK, "Opphør av barnepensjon"),
    BARNEPENSJON_OPPHOER_UTFALL(Brevtype.VEDTAK, "Opphør av barnepensjon"),
    BARNEPENSJON_REVURDERING(Brevtype.VEDTAK),
    BARNEPENSJON_REVURDERING_UTFALL(Brevtype.VEDTAK),
    BARNEPENSJON_FORHAANDSVARSEL_OMREGNING(Brevtype.VEDTAK),
    BARNEPENSJON_VARSEL(Brevtype.VARSEL, "Forhåndsvarsel om ny barnepensjon fra 1. januar 2024"),
    BARNEPENSJON_VARSEL_UTFALL(Brevtype.VARSEL, "Forhåndsvarsel om ny barnepensjon fra 1. januar 2024"),
    BARNEPENSJON_VEDTAK_OMREGNING(Brevtype.VEDTAK, "Vedtak - endring av barnepensjon"),
    BARNEPENSJON_VEDTAK_OMREGNING_FERDIG(Brevtype.VEDTAK, "Vedtak - endring av barnepensjon"),
    BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL(Brevtype.VEDLEGG, "Trygdetid i vedlegg beregning av barnepensjon"),
    BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL(Brevtype.VEDLEGG, "Utfall ved forhåndsvarsel av feilutbetaling"),
    BARNEPENSJON_INFORMASJON_DOEDSFALL(Brevtype.INFORMASJON, "Informasjon om barnepensjon"),

    OMSTILLINGSSTOENAD_INFORMASJON_DOEDSFALL(Brevtype.INFORMASJON, "Informasjon om omstillingsstønad"),
    OMSTILLINGSSTOENAD_AVSLAG(Brevtype.VEDTAK, "Vedtak om avslått omstillingsstønad"),
    OMSTILLINGSSTOENAD_AVSLAG_UTFALL(Brevtype.VEDTAK, "Vedtak om avslått omstillingsstønad"),
    OMSTILLINGSSTOENAD_INNVILGELSE(Brevtype.VEDTAK, "Vedtak om innvilget omstillingsstønad"),
    OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL(Brevtype.VEDTAK, "Vedtak om innvilget omstillingsstønad"),
    OMSTILLINGSSTOENAD_OPPHOER(Brevtype.VEDTAK, "Opphør av omstillingsstønad"),
    OMSTILLINGSSTOENAD_OPPHOER_UTFALL(Brevtype.VEDTAK, "Opphør av omstillingsstønad"),
    OMSTILLINGSSTOENAD_REVURDERING(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_REVURDERING_UTFALL(Brevtype.VEDTAK),
    OMSTILLINGSSTOENAD_VARSEL(Brevtype.VARSEL, "Varsel - omstillingsstønad"),
    OMSTILLINGSSTOENAD_VARSEL_UTFALL(Brevtype.VARSEL, "Varsel - omstillingsstønad"),
    OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL(Brevtype.VEDLEGG, "Utfall ved beregning av omstillingsstønad"),
    OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL(Brevtype.VEDLEGG, "Utfall ved forhåndsvarsel av feilutbetaling"),
    TILBAKEKREVING_INNHOLD(Brevtype.VEDTAK),
    TILBAKEKREVING_FERDIG(Brevtype.VEDTAK),

    AVVIST_KLAGE_INNHOLD(Brevtype.VEDTAK),
    AVVIST_KLAGE_FERDIG(Brevtype.VEDTAK),

    TOM_DELMAL(Brevtype.MANUELT),
    TOM_MAL_INFORMASJONSBREV(Brevtype.INFORMASJON, "Informasjonsbrev"),
    TOM_MAL(Brevtype.MANUELT),
    UTSATT_KLAGEFRIST(Brevtype.INFORMASJON, "Informasjon om barnepensjon fra 1. januar 2024"),

    KLAGE_OVERSENDELSE_BRUKER(Brevtype.OVERSENDELSE_KLAGE, "Klagen er oversendt til NAV Klageinstans Vest"),
    KLAGE_OVERSENDELSE_BLANKETT(Brevtype.NOTAT, "Oversendelse til KA"),
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
    ;

    fun erKobletTilEnBehandling(): Boolean {
        return this in listOf(VEDTAK, VARSEL, VEDLEGG)
    }
}
