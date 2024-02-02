package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_OPPHOER_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VARSEL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VARSEL_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_INNHOLD
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_DELMAL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV

data class Brevkoder(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering) {
    companion object {
        val OMREGNING =
            Brevkoder(
                BARNEPENSJON_VEDTAK_OMREGNING,
                BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
            )
        val TILBAKEKREVING =
            Brevkoder(
                TILBAKEKREVING_INNHOLD,
                TILBAKEKREVING_FERDIG,
            )
        val TOMT_INFORMASJONSBREV =
            Brevkoder(
                TOM_DELMAL,
                TOM_MAL_INFORMASJONSBREV,
            )
    }

    object Barnepensjon {
        val AVSLAG = Brevkoder(BARNEPENSJON_AVSLAG_UTFALL, BARNEPENSJON_AVSLAG)
        val INNVILGELSE =
            Brevkoder(
                BARNEPENSJON_INNVILGELSE_UTFALL,
                BARNEPENSJON_INNVILGELSE,
            )
        val OPPHOER = Brevkoder(BARNEPENSJON_OPPHOER_UTFALL, BARNEPENSJON_OPPHOER)
        val REVURDERING = Brevkoder(BARNEPENSJON_REVURDERING_UTFALL, BARNEPENSJON_REVURDERING)
        val VARSEL = Brevkoder(BARNEPENSJON_VARSEL_UTFALL, BARNEPENSJON_VARSEL)
    }

    object Omstillingsstoenad {
        val AVSLAG =
            Brevkoder(
                OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
                OMSTILLINGSSTOENAD_AVSLAG,
            )
        val INNVILGELSE =
            Brevkoder(
                OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
                OMSTILLINGSSTOENAD_INNVILGELSE,
            )
        val OPPHOER =
            Brevkoder(
                OMSTILLINGSSTOENAD_OPPHOER_UTFALL,
                OMSTILLINGSSTOENAD_OPPHOER,
            )
        val REVURDERING = Brevkoder(TOM_MAL, OMSTILLINGSSTOENAD_REVURDERING)
        val VARSEL = Brevkoder(OMSTILLINGSSTOENAD_VARSEL_UTFALL, OMSTILLINGSSTOENAD_VARSEL)
    }
}
