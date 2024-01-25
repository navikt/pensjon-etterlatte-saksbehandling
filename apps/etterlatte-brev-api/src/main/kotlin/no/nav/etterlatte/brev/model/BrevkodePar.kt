package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode

data class BrevkodePar(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering) {
    companion object {
        val OMREGNING =
            BrevkodePar(
                EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING,
                EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
            )
        val TILBAKEKREVING =
            BrevkodePar(
                EtterlatteBrevKode.TILBAKEKREVING_INNHOLD,
                EtterlatteBrevKode.TILBAKEKREVING_FERDIG,
            )
        val TOMT_INFORMASJONSBREV =
            BrevkodePar(
                EtterlatteBrevKode.TOM_DELMAL,
                EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
            )
    }

    object Barnepensjon {
        val AVSLAG = BrevkodePar(EtterlatteBrevKode.BARNEPENSJON_AVSLAG_UTFALL, EtterlatteBrevKode.BARNEPENSJON_AVSLAG)
        val INNVILGELSE =
            BrevkodePar(
                EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL,
                EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE,
            )
        val OPPHOER = BrevkodePar(EtterlatteBrevKode.TOM_MAL, EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OPPHOER)
        val REVURDERING = BrevkodePar(EtterlatteBrevKode.TOM_MAL, EtterlatteBrevKode.BARNEPENSJON_REVURDERING)
    }

    object Omstillingsstoenad {
        val AVSLAG =
            BrevkodePar(
                EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
                EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG,
            )
        val INNVILGELSE =
            BrevkodePar(
                EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
                EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE,
            )
        val OPPHOER =
            BrevkodePar(
                EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_UTFALL,
                EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER,
            )
        val REVURDERING = BrevkodePar(EtterlatteBrevKode.TOM_MAL, EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING)
    }
}
