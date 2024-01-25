package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_INNHOLD
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_DELMAL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV

data class BrevkodePar(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering) {
    companion object {
        val OMREGNING =
            BrevkodePar(
                BARNEPENSJON_VEDTAK_OMREGNING,
                BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
            )
        val TILBAKEKREVING =
            BrevkodePar(
                TILBAKEKREVING_INNHOLD,
                TILBAKEKREVING_FERDIG,
            )
        val TOMT_INFORMASJONSBREV =
            BrevkodePar(
                TOM_DELMAL,
                TOM_MAL_INFORMASJONSBREV,
            )
    }

    object Barnepensjon {
        val AVSLAG = BrevkodePar(BARNEPENSJON_AVSLAG_UTFALL, BARNEPENSJON_AVSLAG)
        val INNVILGELSE =
            BrevkodePar(
                BARNEPENSJON_INNVILGELSE_UTFALL,
                BARNEPENSJON_INNVILGELSE,
            )
        val OPPHOER = BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_OPPHOER)
        val REVURDERING = BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING)
    }

    object Omstillingsstoenad {
        val AVSLAG =
            BrevkodePar(
                OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
                OMSTILLINGSSTOENAD_AVSLAG,
            )
        val INNVILGELSE =
            BrevkodePar(
                OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
                OMSTILLINGSSTOENAD_INNVILGELSE,
            )
        val OPPHOER =
            BrevkodePar(
                OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_UTFALL,
                OMSTILLINGSSTOENAD_REVURDERING_OPPHOER,
            )
        val REVURDERING = BrevkodePar(TOM_MAL, OMSTILLINGSSTOENAD_REVURDERING)
    }
}
