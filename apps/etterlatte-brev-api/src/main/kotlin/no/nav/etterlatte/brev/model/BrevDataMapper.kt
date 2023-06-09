package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

object BrevDataMapper {
    fun fra(
        behandling: Behandling
    ): Pair<EtterlatteBrevKode, BrevData> = when (behandling.sakType) {
        SakType.BARNEPENSJON -> {
            when (val vedtakType = behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> Pair(
                    EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE,
                    InnvilgetBrevData.fra(
                        behandling
                    )
                )
                VedtakType.AVSLAG -> Pair(
                    EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE,
                    AvslagBrevData.fra(
                        behandling
                    )
                )
                VedtakType.ENDRING -> Pair(
                    EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE, // TODO: Legg til brevkode for endring
                    EndringBrevData.fra(
                        behandling
                    )
                )
                VedtakType.OPPHOER -> TODO("Vedtakstype er ikke støttet: $vedtakType")
            }
        }
        SakType.OMSTILLINGSSTOENAD -> {
            when (val vedtakType = behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> Pair(
                    EtterlatteBrevKode.OMS_INNVILGELSE_AUTO,
                    InnvilgetBrevData.fra(
                        behandling
                    )
                )
                VedtakType.AVSLAG -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                VedtakType.ENDRING -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                VedtakType.OPPHOER -> TODO("Vedtakstype er ikke støttet: $vedtakType")
            }
        }
    }
}