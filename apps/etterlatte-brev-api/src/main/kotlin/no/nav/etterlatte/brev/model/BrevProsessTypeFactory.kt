package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevProsessTypeFactory(private val featureToggleService: FeatureToggleService) {
    fun fra(generellBrevData: GenerellBrevData): BrevProsessType {
        return when (generellBrevData.sak.sakType) {
            SakType.OMSTILLINGSSTOENAD -> omsBrev(generellBrevData)
            SakType.BARNEPENSJON -> bpBrev(generellBrevData)
        }
    }

    private fun omsBrev(generellBrevData: GenerellBrevData): BrevProsessType {
        return when (generellBrevData.forenkletVedtak.type) {
            VedtakType.INNVILGELSE -> BrevProsessType.REDIGERBAR
            VedtakType.OPPHOER ->
                when (generellBrevData.revurderingsaarsak?.redigerbartBrev) {
                    true -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.AVSLAG,
            VedtakType.ENDRING,
            ->
                when (generellBrevData.revurderingsaarsak) {
                    RevurderingAarsak.INNTEKTSENDRING,
                    RevurderingAarsak.ANNEN,
                    -> BrevProsessType.REDIGERBAR

                    else -> BrevProsessType.MANUELL
                }

            VedtakType.TILBAKEKREVING -> TODO("EY-2806")
        }
    }

    private fun bpBrev(generellBrevData: GenerellBrevData): BrevProsessType {
        return when (generellBrevData.forenkletVedtak.type) {
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
                when (generellBrevData.revurderingsaarsak) {
                    RevurderingAarsak.SOESKENJUSTERING -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.FENGSELSOPPHOLD -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.UT_AV_FENGSEL -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.YRKESSKADE -> BrevProsessType.REDIGERBAR
                    RevurderingAarsak.ANNEN -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.OPPHOER ->
                when (generellBrevData.revurderingsaarsak?.redigerbartBrev) {
                    true -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.AVSLAG -> BrevProsessType.MANUELL
            VedtakType.TILBAKEKREVING -> TODO("EY-2806")
        }
    }
}
