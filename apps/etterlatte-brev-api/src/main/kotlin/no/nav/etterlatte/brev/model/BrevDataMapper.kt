package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

object BrevDataMapper {
    fun fra(behandling: Behandling) =
        when (behandling.sakType) {
            SakType.BARNEPENSJON -> {
                when (val vedtakType = behandling.vedtak.type) {
                    VedtakType.INNVILGELSE -> InnvilgetBrevData.fra(behandling)
                    VedtakType.AVSLAG,
                    VedtakType.ENDRING,
                    VedtakType.OPPHOER -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (val vedtakType = behandling.vedtak.type) {
                    VedtakType.INNVILGELSE -> InnvilgetBrevData.fra(behandling)
                    VedtakType.AVSLAG,
                    VedtakType.ENDRING,
                    VedtakType.OPPHOER -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                }
            }
        }
}