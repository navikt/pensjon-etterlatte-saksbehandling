package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.model.Brevtype
import no.nav.etterlatte.brev.model.Brevtype.INFORMASJON
import no.nav.etterlatte.brev.model.Brevtype.MANUELT
import no.nav.etterlatte.brev.model.Brevtype.VARSEL
import no.nav.etterlatte.brev.model.Brevtype.VEDLEGG
import no.nav.etterlatte.brev.model.Brevtype.VEDTAK

enum class EtterlatteBrevKode(val brevtype: Brevtype, val tittel: String? = null) {
    BARNEPENSJON_AVSLAG(VEDTAK, "Vedtak om avslått barnepensjon"),
    BARNEPENSJON_AVSLAG_UTFALL(VEDTAK),
    BARNEPENSJON_INNVILGELSE(VEDTAK, "Vedtak om innvilget barnepensjon"),
    BARNEPENSJON_INNVILGELSE_UTFALL(VEDTAK),
    BARNEPENSJON_OPPHOER(VEDTAK, "Opphør av barnepensjon"),
    BARNEPENSJON_OPPHOER_UTFALL(VEDTAK),
    BARNEPENSJON_REVURDERING(VEDTAK),
    BARNEPENSJON_REVURDERING_UTFALL(VEDTAK),
    BARNEPENSJON_FORHAANDSVARSEL_OMREGNING(VEDTAK),
    BARNEPENSJON_VARSEL(VARSEL),
    BARNEPENSJON_VARSEL_UTFALL(VARSEL),
    BARNEPENSJON_VEDTAK_OMREGNING(VEDTAK, "Vedtak - endring av barnepensjon"),
    BARNEPENSJON_VEDTAK_OMREGNING_FERDIG(VEDTAK),
    BARNEPENSJON_VEDLEGG_BEREGNING_TRYGDETID_UTFALL(VEDLEGG, "Trygdetid i vedlegg beregning av barnepensjon"),

    OMSTILLINGSSTOENAD_AVSLAG(VEDTAK, "Vedtak om avslått omstillingsstønad"),
    OMSTILLINGSSTOENAD_AVSLAG_UTFALL(VEDTAK),
    OMSTILLINGSSTOENAD_INNVILGELSE(VEDTAK, "Vedtak om innvilget omstillingsstønad"),
    OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL(VEDTAK),
    OMSTILLINGSSTOENAD_OPPHOER(VEDTAK, "Opphør av omstillingsstønad"),
    OMSTILLINGSSTOENAD_OPPHOER_UTFALL(VEDTAK),
    OMSTILLINGSSTOENAD_REVURDERING(VEDTAK),
    OMSTILLINGSSTOENAD_VARSEL(VARSEL),
    OMSTILLINGSSTOENAD_VARSEL_UTFALL(VARSEL),
    OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL(VEDLEGG, "Utfall ved beregning av omstillingsstønad"),
    OMSTILLINGSSTOENAD_VEDLEGG_FORHAANDSVARSEL_UTFALL(VEDLEGG, "Utfall ved forhåndsvarsel av feilutbetaling"),
    TILBAKEKREVING_INNHOLD(VEDTAK),
    TILBAKEKREVING_FERDIG(VEDTAK),

    AVVIST_KLAGE_INNHOLD(VEDTAK),
    AVVIST_KLAGE_FERDIG(VEDTAK),

    TOM_DELMAL(MANUELT),
    TOM_MAL_INFORMASJONSBREV(INFORMASJON),
    TOM_MAL(MANUELT),
    UTSATT_KLAGEFRIST(INFORMASJON, "Informasjon om barnepensjon fra 1. januar 2024"),
}
