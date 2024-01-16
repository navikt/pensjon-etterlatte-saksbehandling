package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevProsessTypeFactory(private val featureToggleService: FeatureToggleService) {
    fun fra(
        generellBrevData: GenerellBrevData,
        erOmregningNyRegel: Boolean = false,
    ): BrevProsessType {
        return when (generellBrevData.sak.sakType) {
            SakType.OMSTILLINGSSTOENAD -> omsBrev(generellBrevData)
            SakType.BARNEPENSJON -> bpBrev(generellBrevData, erOmregningNyRegel)
        }
    }

    private fun omsBrev(generellBrevData: GenerellBrevData): BrevProsessType {
        return when (generellBrevData.forenkletVedtak?.type) {
            VedtakType.INNVILGELSE -> BrevProsessType.REDIGERBAR
            VedtakType.OPPHOER ->
                when (generellBrevData.revurderingsaarsak?.redigerbartBrev) {
                    true -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.AVSLAG -> BrevProsessType.REDIGERBAR
            VedtakType.ENDRING,
            ->
                when (generellBrevData.revurderingsaarsak) {
                    Revurderingaarsak.INNTEKTSENDRING,
                    Revurderingaarsak.ANNEN,
                    -> BrevProsessType.REDIGERBAR

                    else -> BrevProsessType.MANUELL
                }

            VedtakType.TILBAKEKREVING -> BrevProsessType.REDIGERBAR
            null -> BrevProsessType.REDIGERBAR
        }
    }

    private fun bpBrev(
        generellBrevData: GenerellBrevData,
        erOmregningNyRegel: Boolean = false,
    ): BrevProsessType {
        if (generellBrevData.erMigrering() || erOmregningNyRegel) {
            return BrevProsessType.REDIGERBAR
        }
        return when (generellBrevData.forenkletVedtak?.type) {
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
                    Revurderingaarsak.SOESKENJUSTERING -> BrevProsessType.REDIGERBAR
                    Revurderingaarsak.FENGSELSOPPHOLD -> BrevProsessType.REDIGERBAR
                    Revurderingaarsak.UT_AV_FENGSEL -> BrevProsessType.REDIGERBAR
                    Revurderingaarsak.YRKESSKADE -> BrevProsessType.REDIGERBAR
                    Revurderingaarsak.ANNEN -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.OPPHOER ->
                when (generellBrevData.revurderingsaarsak?.redigerbartBrev) {
                    true -> BrevProsessType.REDIGERBAR
                    else -> BrevProsessType.MANUELL
                }

            VedtakType.AVSLAG -> BrevProsessType.REDIGERBAR
            VedtakType.TILBAKEKREVING -> BrevProsessType.REDIGERBAR
            null -> BrevProsessType.REDIGERBAR
        }
    }
}
