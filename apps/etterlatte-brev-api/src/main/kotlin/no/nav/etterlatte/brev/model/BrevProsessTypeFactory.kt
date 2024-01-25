package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevProsessTypeFactory {
    fun fra(generellBrevData: GenerellBrevData): BrevProsessType {
        return when (generellBrevData.sak.sakType) {
            SakType.OMSTILLINGSSTOENAD -> omsBrev(generellBrevData)
            SakType.BARNEPENSJON -> bpBrev(generellBrevData)
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

    private fun bpBrev(generellBrevData: GenerellBrevData): BrevProsessType {
        if (generellBrevData.erMigrering()) {
            return BrevProsessType.REDIGERBAR
        }
        return when (generellBrevData.forenkletVedtak?.type) {
            VedtakType.INNVILGELSE ->
                BrevProsessType.REDIGERBAR

            VedtakType.ENDRING ->
                BrevProsessType.REDIGERBAR

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
