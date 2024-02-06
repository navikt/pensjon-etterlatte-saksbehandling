package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

data class BrevkodeRequest(val erMigrering: Boolean, val sakType: SakType, val vedtakType: VedtakType?)

class BrevKodeMapperVedtak {
    fun brevKode(generellBrevData: BrevkodeRequest): Brevkoder {
        if (generellBrevData.erMigrering) {
            assert(listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING).contains(generellBrevData.vedtakType))
            return Brevkoder.OMREGNING
        }

        return when (generellBrevData.sakType) {
            SakType.BARNEPENSJON -> {
                when (generellBrevData.vedtakType) {
                    VedtakType.INNVILGELSE -> Brevkoder.BP_INNVILGELSE
                    VedtakType.AVSLAG -> Brevkoder.BP_AVSLAG
                    VedtakType.ENDRING -> Brevkoder.BP_REVURDERING
                    VedtakType.OPPHOER -> Brevkoder.BP_OPPHOER
                    VedtakType.TILBAKEKREVING -> Brevkoder.TILBAKEKREVING
                    null -> TODO()
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.vedtakType) {
                    VedtakType.INNVILGELSE -> Brevkoder.OMS_INNVILGELSE
                    VedtakType.AVSLAG -> Brevkoder.OMS_AVSLAG
                    VedtakType.ENDRING -> Brevkoder.OMS_REVURDERING
                    VedtakType.OPPHOER -> Brevkoder.OMS_OPPHOER
                    VedtakType.TILBAKEKREVING -> Brevkoder.TILBAKEKREVING
                    null -> TODO()
                }
            }
        }
    }
}
