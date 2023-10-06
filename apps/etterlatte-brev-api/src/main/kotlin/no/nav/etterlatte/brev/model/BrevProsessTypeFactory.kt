package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevProsessTypeFactory(private val featureToggleService: FeatureToggleService) {
    fun fra(behandling: Behandling): BrevProsessType {
        return when (behandling.sakType) {
            SakType.OMSTILLINGSSTOENAD -> omsBrev(behandling)
            SakType.BARNEPENSJON -> bpBrev(behandling)
        }
    }

    private fun omsBrev(behandling: Behandling): BrevProsessType {
        return when (behandling.vedtak.type) {
            VedtakType.INNVILGELSE -> BrevProsessType.REDIGERBAR
            VedtakType.OPPHOER ->
                when (behandling.revurderingsaarsak?.redigerbartBrev) {
                    true -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.AVSLAG,
            VedtakType.ENDRING,
            ->
                when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.INNTEKTSENDRING,
                    RevurderingAarsak.ANNEN,
                    -> BrevProsessType.REDIGERBAR

                    else -> BrevProsessType.MANUELL
                }
            VedtakType.TILBAKEKREVING -> TODO("EY-2806")
        }
    }

    private fun bpBrev(behandling: Behandling): BrevProsessType {
        return when (behandling.vedtak.type) {
            VedtakType.INNVILGELSE ->
                when (
                    featureToggleService.isEnabled(
                        BrevDataFeatureToggle.NyMalInnvilgelse,
                        false,
                    )
                ) {
                    true -> BrevProsessType.REDIGERBAR
                    false -> BrevProsessType.AUTOMATISK
                }

            VedtakType.ENDRING ->
                when (behandling.revurderingsaarsak) {
                    RevurderingAarsak.SOESKENJUSTERING -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.FENGSELSOPPHOLD -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.UT_AV_FENGSEL -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.YRKESSKADE -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.ANNEN -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.OPPHOER ->
                when (behandling.revurderingsaarsak?.redigerbartBrev) {
                    true -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.AVSLAG -> BrevProsessType.MANUELL
            VedtakType.TILBAKEKREVING -> TODO("EY-2806")
        }
    }
}
