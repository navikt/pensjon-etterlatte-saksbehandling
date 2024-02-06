package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevKodeMapper {
    fun brevKode(generellBrevData: GenerellBrevData): Brevkoder =
        when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> Brevkoder.BP_INNVILGELSE
                    VedtakType.AVSLAG -> Brevkoder.BP_AVSLAG
                    VedtakType.ENDRING -> Brevkoder.BP_REVURDERING
                    VedtakType.OPPHOER -> Brevkoder.BP_OPPHOER
                    VedtakType.TILBAKEKREVING -> Brevkoder.TILBAKEKREVING
                    VedtakType.AVVIST_KLAGE -> Brevkoder.AVVIST_KLAGE
                    null -> Brevkoder.TOMT_INFORMASJONSBREV
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> Brevkoder.OMS_INNVILGELSE
                    VedtakType.AVSLAG -> Brevkoder.OMS_AVSLAG
                    VedtakType.ENDRING -> Brevkoder.OMS_REVURDERING
                    VedtakType.OPPHOER -> Brevkoder.OMS_OPPHOER
                    VedtakType.TILBAKEKREVING -> Brevkoder.TILBAKEKREVING
                    VedtakType.AVVIST_KLAGE -> Brevkoder.AVVIST_KLAGE
                    null -> Brevkoder.TOMT_INFORMASJONSBREV
                }
            }
        }
}
