package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevKodeAutomatiskMapper(private val featureToggleService: FeatureToggleService) {
    internal fun brevKodeAutomatisk(generellBrevData: GenerellBrevData): BrevDataMapper.BrevkodePar {
        if (generellBrevData.systemkilde == Vedtaksloesning.PESYS) {
            assert(generellBrevData.forenkletVedtak.type == VedtakType.INNVILGELSE)
            return BrevDataMapper.BrevkodePar(EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING)
        }

        return when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE ->
                        when (brukNyInnvilgelsesmal()) {
                            true ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_ENKEL,
                                    EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_NY,
                                )
                            false -> BrevDataMapper.BrevkodePar(EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE)
                        }

                    VedtakType.AVSLAG ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.YRKESSKADE ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.BARNEPENSJON_AVSLAG_IKKEYRKESSKADE,
                                    EtterlatteBrevKode.BARNEPENSJON_AVSLAG,
                                )

                            else -> BrevDataMapper.BrevkodePar(EtterlatteBrevKode.BARNEPENSJON_AVSLAG)
                        }

                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SOESKENJUSTERING ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_SOESKENJUSTERING,
                                )
                            Revurderingaarsak.INSTITUSJONSOPPHOLD ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            Revurderingaarsak.YRKESSKADE ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            Revurderingaarsak.ANNEN ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.ADOPSJON ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ADOPSJON,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OPPHOER,
                                )

                            Revurderingaarsak.OMGJOERING_AV_FARSKAP ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OPPHOER,
                                )

                            Revurderingaarsak.FENGSELSOPPHOLD ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            Revurderingaarsak.UT_AV_FENGSEL ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                        }

                    VedtakType.TILBAKEKREVING ->
                        BrevDataMapper.BrevkodePar(
                            EtterlatteBrevKode.TILBAKEKREVING_INNHOLD,
                            EtterlatteBrevKode.TILBAKEKREVING_FERDIG,
                        )
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE ->
                        BrevDataMapper.BrevkodePar(
                            EtterlatteBrevKode.OMS_FOERSTEGANGSVEDTAK_INNVILGELSE_UTFALL,
                            EtterlatteBrevKode.OMS_FOERSTEGANGSVEDTAK_INNVILGELSE,
                        )

                    VedtakType.AVSLAG ->
                        BrevDataMapper.BrevkodePar(
                            EtterlatteBrevKode.OMS_AVSLAG_BEGRUNNELSE,
                            EtterlatteBrevKode.OMS_AVSLAG,
                        )
                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.INNTEKTSENDRING,
                            Revurderingaarsak.ANNEN,
                            ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.OMS_REVURDERING_ENDRING,
                                )

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SIVILSTAND ->
                                BrevDataMapper.BrevkodePar(
                                    EtterlatteBrevKode.OMS_REVURDERING_OPPHOER_GENERELL,
                                    EtterlatteBrevKode.OMS_REVURDERING_OPPHOER,
                                )

                            else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                        }

                    VedtakType.TILBAKEKREVING ->
                        BrevDataMapper.BrevkodePar(
                            EtterlatteBrevKode.TILBAKEKREVING_INNHOLD,
                            EtterlatteBrevKode.TILBAKEKREVING_FERDIG,
                        )
                }
            }
        }
    }

    private fun brukNyInnvilgelsesmal() = featureToggleService.isEnabled(BrevDataFeatureToggle.NyMalInnvilgelse, false)
}
