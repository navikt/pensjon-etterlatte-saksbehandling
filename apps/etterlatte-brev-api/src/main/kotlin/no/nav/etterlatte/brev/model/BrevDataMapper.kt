package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
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
                    EtterlatteBrevKode.BARNEPENSJON_AVSLAG,
                    AvslagBrevData.fra(behandling)
                )
                VedtakType.ENDRING -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.SOESKENJUSTERING -> Pair(
                        EtterlatteBrevKode.BARNEPENSJON_REVURDERING_SOESKENJUSTERING,
                        SoeskenjusteringRevurderingBrevdata.fra(behandling)
                    )
                    else -> TODO("Revurderingsbrev for ${behandling.revurderingsaarsak} er ikke støttet")
                }
                VedtakType.OPPHOER -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.ADOPSJON -> Pair(
                        EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ADOPSJON,
                        AdopsjonRevurderingBrevdata.fra(behandling)
                    )
                    RevurderingAarsak.OMGJOERING_AV_FARSKAP -> Pair(
                        EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP,
                        OmgjoeringAvFarskapRevurderingBrevdata.fra(behandling)
                    )
                    else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                }
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