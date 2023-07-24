package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

object BrevDataMapper {

    fun brevKode(behandling: Behandling, brevProsessType: BrevProsessType) = when (brevProsessType) {
        BrevProsessType.AUTOMATISK -> brevKodeAutomatisk(behandling)
        BrevProsessType.MANUELL -> EtterlatteBrevKode.OMS_OPPHOER_MANUELL
    }

    private fun brevKodeAutomatisk(behandling: Behandling): EtterlatteBrevKode = when (behandling.sakType) {
        SakType.BARNEPENSJON -> {
            when (val vedtakType = behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
                VedtakType.AVSLAG -> EtterlatteBrevKode.BARNEPENSJON_AVSLAG
                VedtakType.ENDRING -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.SOESKENJUSTERING -> EtterlatteBrevKode.BARNEPENSJON_REVURDERING_SOESKENJUSTERING
                    else -> TODO("Revurderingsbrev for ${behandling.revurderingsaarsak} er ikke støttet")
                }

                VedtakType.OPPHOER -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.ADOPSJON -> EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ADOPSJON
                    RevurderingAarsak.OMGJOERING_AV_FARSKAP ->
                        EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP
                    else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                }
            }
        }

        SakType.OMSTILLINGSSTOENAD -> {
            when (val vedtakType = behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> EtterlatteBrevKode.OMS_INNVILGELSE_AUTO
                VedtakType.AVSLAG -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                VedtakType.ENDRING -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                VedtakType.OPPHOER -> TODO("Vedtakstype er ikke støttet: $vedtakType")
            }
        }
    }

    fun brevData(behandling: Behandling): BrevData = when (behandling.sakType) {
        SakType.BARNEPENSJON -> {
            when (val vedtakType = behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> InnvilgetBrevData.fra(behandling)
                VedtakType.AVSLAG -> AvslagBrevData.fra(behandling)
                VedtakType.ENDRING -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.SOESKENJUSTERING -> SoeskenjusteringRevurderingBrevdata.fra(behandling)
                    else -> TODO("Revurderingsbrev for ${behandling.revurderingsaarsak} er ikke støttet")
                }

                VedtakType.OPPHOER -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.ADOPSJON -> AdopsjonRevurderingBrevdata.fra(behandling)
                    RevurderingAarsak.OMGJOERING_AV_FARSKAP -> OmgjoeringAvFarskapRevurderingBrevdata.fra(behandling)
                    else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                }
            }
        }

        SakType.OMSTILLINGSSTOENAD -> {
            when (val vedtakType = behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> InnvilgetBrevData.fra(behandling)
                VedtakType.AVSLAG -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                VedtakType.ENDRING -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                VedtakType.OPPHOER -> TODO("Vedtakstype er ikke støttet: $vedtakType")
            }
        }
    }
}