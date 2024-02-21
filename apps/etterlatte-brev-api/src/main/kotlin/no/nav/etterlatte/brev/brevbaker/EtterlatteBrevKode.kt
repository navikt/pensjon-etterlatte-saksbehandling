package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.model.Brevtype
import no.nav.etterlatte.brev.model.Brevtype.INFORMASJON
import no.nav.etterlatte.brev.model.Brevtype.MANUELT
import no.nav.etterlatte.brev.model.Brevtype.NOTAT
import no.nav.etterlatte.brev.model.Brevtype.VARSEL
import no.nav.etterlatte.brev.model.Brevtype.VEDLEGG
import no.nav.etterlatte.brev.model.Brevtype.VEDTAK

enum class EtterlatteBrevKode(val brevtype: Brevtype, val tittel: String? = null) {
    BARNEPENSJON_AVSLAG(VEDTAK, "Vedtak om avslått barnepensjon"),
    BARNEPENSJON_AVSLAG_UTFALL(VEDTAK, "Vedtak om avslått barnepensjon"),
    BARNEPENSJON_INNVILGELSE(VEDTAK, "Vedtak om innvilget barnepensjon"),
    BARNEPENSJON_INNVILGELSE_UTFALL(VEDTAK, "Vedtak om innvilget barnepensjon"),
    BARNEPENSJON_INNVILGELSE_FORELDRELOES(VEDTAK, "Vedtak om innvilget barnepensjon"),
    BARNEPENSJON_INNVILGELSE_UTFALL_FORELDRELOES(VEDTAK, "Vedtak om innvilget barnepensjon"),
    BARNEPENSJON_OPPHOER(VEDTAK, "Opphør av barnepensjon"),
    BARNEPENSJON_OPPHOER_UTFALL(VEDTAK, "Opphør av barnepensjon"),
    BARNEPENSJON_REVURDERING(VEDTAK),
    BARNEPENSJON_REVURDERING_UTFALL(VEDTAK),
    BARNEPENSJON_FORHAANDSVARSEL_OMREGNING(VEDTAK),
    BARNEPENSJON_VARSEL(VARSEL, "Forhåndsvarsel om ny barnepensjon fra 1. januar 2024"),
    BARNEPENSJON_VARSEL_UTFALL(VARSEL, "Forhåndsvarsel om ny barnepensjon fra 1. januar 2024"),
    BARNEPENSJON_VEDTAK_OMREGNING(VEDTAK, "Vedtak - endring av barnepensjon"),
    BARNEPENSJON_VEDTAK_OMREGNING_FERDIG(VEDTAK, "Vedtak - endring av barnepensjon"),
    BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL(VEDLEGG, "Trygdetid i vedlegg beregning av barnepensjon"),
    BARNEPENSJON_VEDLEGG_FORHAANDSVARSEL_UTFALL(VEDLEGG, "Utfall ved forhåndsvarsel av feilutbetaling"),
    BARNEPENSJON_INFORMASJON_DOEDSFALL(INFORMASJON, "Informasjon om barnepensjon"),

    OMSTILLINGSSTOENAD_AVSLAG(VEDTAK, "Vedtak om avslått omstillingsstønad"),
    OMSTILLINGSSTOENAD_AVSLAG_UTFALL(VEDTAK, "Vedtak om avslått omstillingsstønad"),
    OMSTILLINGSSTOENAD_INNVILGELSE(VEDTAK, "Vedtak om innvilget omstillingsstønad"),
    OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL(VEDTAK, "Vedtak om innvilget omstillingsstønad"),
    OMSTILLINGSSTOENAD_OPPHOER(VEDTAK, "Opphør av omstillingsstønad"),
    OMSTILLINGSSTOENAD_OPPHOER_UTFALL(VEDTAK, "Opphør av omstillingsstønad"),
    OMSTILLINGSSTOENAD_REVURDERING(VEDTAK),
    OMSTILLINGSSTOENAD_REVURDERING_UTFALL(VEDTAK),
    OMSTILLINGSSTOENAD_VARSEL(VARSEL, "Varsel - omstillingsstønad"),
    OMSTILLINGSSTOENAD_VARSEL_UTFALL(VARSEL, "Varsel - omstillingsstønad"),
    OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL(VEDLEGG, "Utfall ved beregning av omstillingsstønad"),
    OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL(VEDLEGG, "Utfall ved forhåndsvarsel av feilutbetaling"),
    TILBAKEKREVING_INNHOLD(VEDTAK),
    TILBAKEKREVING_FERDIG(VEDTAK),

    AVVIST_KLAGE_INNHOLD(VEDTAK),
    AVVIST_KLAGE_FERDIG(VEDTAK),

    TOM_DELMAL(MANUELT),
    TOM_MAL_INFORMASJONSBREV(INFORMASJON, "Informasjonsbrev"),
    TOM_MAL(MANUELT),
    UTSATT_KLAGEFRIST(INFORMASJON, "Informasjon om barnepensjon fra 1. januar 2024"),

    KLAGE_OVERSENDELSE(NOTAT, "Oversendelse til KA"),
}
