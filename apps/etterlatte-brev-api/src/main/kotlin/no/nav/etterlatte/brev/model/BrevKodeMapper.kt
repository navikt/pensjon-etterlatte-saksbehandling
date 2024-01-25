package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_OPPHOER
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
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_MANUELL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_INNHOLD
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_DELMAL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevKodeMapper {
    fun brevKode(
        generellBrevData: GenerellBrevData,
        brevProsessType: BrevProsessType,
        erOmregningNyRegel: Boolean = false,
    ) = when (brevProsessType) {
        BrevProsessType.AUTOMATISK -> brevKodeAutomatisk(generellBrevData, erOmregningNyRegel)
        BrevProsessType.REDIGERBAR -> brevKodeAutomatisk(generellBrevData, erOmregningNyRegel)
        BrevProsessType.MANUELL -> BrevkodePar(OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_MANUELL)
        BrevProsessType.OPPLASTET_PDF -> throw IllegalStateException("Brevkode ikke relevant for ${BrevProsessType.OPPLASTET_PDF}")
    }

    private fun brevKodeAutomatisk(
        generellBrevData: GenerellBrevData,
        erOmregningNyRegel: Boolean = false,
    ): BrevkodePar {
        if (generellBrevData.erMigrering() || erOmregningNyRegel) {
            assert(listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING).contains(generellBrevData.forenkletVedtak?.type))
            return BrevkodePar(
                BARNEPENSJON_VEDTAK_OMREGNING,
                BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
            )
        }

        return when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> BrevkodePar.Barnepensjon.INNVILGELSE
                    VedtakType.AVSLAG -> BrevkodePar.Barnepensjon.AVSLAG
                    VedtakType.ENDRING -> BrevkodePar.Barnepensjon.REVURDERING
                    VedtakType.OPPHOER -> BrevkodePar.Barnepensjon.OPPHOER
                    VedtakType.TILBAKEKREVING -> BrevkodePar.TILBAKEKREVING
                    null -> BrevkodePar.TOMT_INFORMASJONSBREV
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> BrevkodePar.Omstillingsstoenad.INNVILGELSE
                    VedtakType.AVSLAG -> BrevkodePar.Omstillingsstoenad.AVSLAG
                    VedtakType.ENDRING -> BrevkodePar.Omstillingsstoenad.REVURDERING
                    VedtakType.OPPHOER -> BrevkodePar.Omstillingsstoenad.OPPHOER
                    VedtakType.TILBAKEKREVING -> BrevkodePar.TILBAKEKREVING
                    null -> BrevkodePar.TOMT_INFORMASJONSBREV
                }
            }
        }
    }
}

data class BrevkodePar(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering) {
    companion object {
        val TILBAKEKREVING = BrevkodePar(TILBAKEKREVING_INNHOLD, TILBAKEKREVING_FERDIG)
        val TOMT_INFORMASJONSBREV = BrevkodePar(TOM_DELMAL, TOM_MAL_INFORMASJONSBREV)
    }

    object Barnepensjon {
        val AVSLAG = BrevkodePar(BARNEPENSJON_AVSLAG_UTFALL, BARNEPENSJON_AVSLAG)
        val INNVILGELSE = BrevkodePar(BARNEPENSJON_INNVILGELSE_UTFALL, BARNEPENSJON_INNVILGELSE)
        val OPPHOER = BrevkodePar(TOM_MAL, BARNEPENSJON_OPPHOER)
        val REVURDERING = BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING)
    }

    object Omstillingsstoenad {
        val AVSLAG = BrevkodePar(OMSTILLINGSSTOENAD_AVSLAG_UTFALL, OMSTILLINGSSTOENAD_AVSLAG)
        val INNVILGELSE = BrevkodePar(OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL, OMSTILLINGSSTOENAD_INNVILGELSE)
        val OPPHOER = BrevkodePar(OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_UTFALL, OMSTILLINGSSTOENAD_REVURDERING_OPPHOER)
        val REVURDERING = BrevkodePar(TOM_MAL, OMSTILLINGSSTOENAD_REVURDERING)
    }
}
