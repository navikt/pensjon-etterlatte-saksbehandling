package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_MANUELL
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
            return BrevkodePar.OMREGNING
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
