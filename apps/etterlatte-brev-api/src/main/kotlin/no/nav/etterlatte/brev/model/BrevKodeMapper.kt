package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevKodeMapper {
    fun brevKode(
        generellBrevData: GenerellBrevData,
        erOmregningNyRegel: Boolean = false,
    ): Brevkoder {
        if (generellBrevData.erMigrering() || erOmregningNyRegel) {
            assert(listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING).contains(generellBrevData.forenkletVedtak?.type))
            return Brevkoder.OMREGNING
        }

        return when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> Brevkoder.Barnepensjon.INNVILGELSE
                    VedtakType.AVSLAG -> Brevkoder.Barnepensjon.AVSLAG
                    VedtakType.ENDRING -> Brevkoder.Barnepensjon.REVURDERING
                    VedtakType.OPPHOER -> Brevkoder.Barnepensjon.OPPHOER
                    VedtakType.TILBAKEKREVING -> Brevkoder.TILBAKEKREVING
                    null -> Brevkoder.TOMT_INFORMASJONSBREV
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> Brevkoder.Omstillingsstoenad.INNVILGELSE
                    VedtakType.AVSLAG -> Brevkoder.Omstillingsstoenad.AVSLAG
                    VedtakType.ENDRING -> Brevkoder.Omstillingsstoenad.REVURDERING
                    VedtakType.OPPHOER -> Brevkoder.Omstillingsstoenad.OPPHOER
                    VedtakType.TILBAKEKREVING -> Brevkoder.TILBAKEKREVING
                    null -> Brevkoder.TOMT_INFORMASJONSBREV
                }
            }
        }
    }
}
