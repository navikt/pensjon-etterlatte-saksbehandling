package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG_IKKEYRKESSKADE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_NY
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ADOPSJON
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_FENGSELSOPPHOLD
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_HAR_STANSET
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_SOESKENJUSTERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_UT_AV_FENGSEL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_INNVILGELSE_AUTO
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_OPPHOER_MANUELL
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

enum class BrevDataFeatureToggle(private val key: String) : FeatureToggle {
    NyMalInnvilgelse("pensjon-etterlatte.bp-ny-mal-innvilgelse");

    override fun key() = key
}

class BrevDataMapper(private val featureToggleService: FeatureToggleService) {

    fun brevKode(behandling: Behandling, brevProsessType: BrevProsessType) = when (brevProsessType) {
        BrevProsessType.AUTOMATISK -> brevKodeAutomatisk(behandling)
        BrevProsessType.REDIGERBAR -> brevKodeAutomatisk(behandling)
        BrevProsessType.MANUELL -> BrevkodePar(OMS_OPPHOER_MANUELL)
    }

    private fun brevKodeAutomatisk(behandling: Behandling): BrevkodePar = when (behandling.sakType) {
        SakType.BARNEPENSJON -> {
            when (val vedtakType = behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> when (
                    featureToggleService.isEnabled(
                        BrevDataFeatureToggle.NyMalInnvilgelse,
                        false
                    )
                ) {
                    true -> BrevkodePar(BARNEPENSJON_INNVILGELSE_NY)
                    false -> BrevkodePar(BARNEPENSJON_INNVILGELSE)
                }

                VedtakType.AVSLAG -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.YRKESSKADE -> BrevkodePar(BARNEPENSJON_AVSLAG_IKKEYRKESSKADE, BARNEPENSJON_AVSLAG)
                    else -> BrevkodePar(BARNEPENSJON_AVSLAG)
                }

                VedtakType.ENDRING -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.SOESKENJUSTERING -> BrevkodePar(BARNEPENSJON_REVURDERING_SOESKENJUSTERING)
                    else -> TODO("Revurderingsbrev for ${behandling.revurderingsaarsak} er ikke støttet")
                }

                VedtakType.OPPHOER -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.ADOPSJON ->
                        BrevkodePar(BARNEPENSJON_REVURDERING_ADOPSJON, BARNEPENSJON_REVURDERING_OPPHOER)

                    RevurderingAarsak.OMGJOERING_AV_FARSKAP ->
                        BrevkodePar(BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP, BARNEPENSJON_REVURDERING_OPPHOER)

                    RevurderingAarsak.FENGSELSOPPHOLD ->
                        BrevkodePar(BARNEPENSJON_REVURDERING_FENGSELSOPPHOLD, BARNEPENSJON_REVURDERING_HAR_STANSET)

                    RevurderingAarsak.UT_AV_FENGSEL ->
                        BrevkodePar(BARNEPENSJON_REVURDERING_UT_AV_FENGSEL, BARNEPENSJON_REVURDERING_ENDRING)

                    else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                }
            }
        }

        SakType.OMSTILLINGSSTOENAD -> {
            when (val vedtakType = behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> BrevkodePar(OMS_INNVILGELSE_AUTO)
                VedtakType.AVSLAG -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                VedtakType.ENDRING -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                VedtakType.OPPHOER -> TODO("Vedtakstype er ikke støttet: $vedtakType")
            }
        }
    }

    fun brevData(behandling: Behandling): BrevData = when (behandling.sakType) {
        SakType.BARNEPENSJON -> {
            when (val vedtakType = behandling.vedtak.type) {
                VedtakType.INNVILGELSE -> when (
                    featureToggleService.isEnabled(
                        BrevDataFeatureToggle.NyMalInnvilgelse,
                        false
                    )
                ) {
                    true -> InnvilgetBrevDataNy.fra(behandling)
                    false -> InnvilgetBrevData.fra(behandling)
                }

                VedtakType.AVSLAG -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.YRKESSKADE -> AvslagYrkesskadeBrevData.fra(behandling)
                    else -> AvslagBrevData.fra(behandling)
                }

                VedtakType.ENDRING -> when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.SOESKENJUSTERING -> SoeskenjusteringRevurderingBrevdata.fra(behandling)
                    RevurderingAarsak.FENGSELSOPPHOLD -> FengselsoppholdBrevdata.fra(behandling)
                    RevurderingAarsak.UT_AV_FENGSEL -> UtAvFengselBrevdata.fra(behandling)
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

    fun brevDataFerdigstilling(behandling: Behandling, innhold: () -> List<Slate.Element>, kode: BrevkodePar) =
        when (kode.ferdigstilling) {
            BARNEPENSJON_REVURDERING_ENDRING -> EndringHovedmalBrevData.fra(behandling, innhold())
            else ->
                when (behandling.revurderingsaarsak?.redigerbartBrev) {
                    true -> ManueltBrevData(innhold())
                    else -> brevData(behandling)
                }
        }

    data class BrevkodePar(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering)
}