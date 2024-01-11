package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevKodeMapper(private val featureToggleService: FeatureToggleService) {
    fun brevKode(
        generellBrevData: GenerellBrevData,
        brevProsessType: BrevProsessType,
        erOmregningNyRegel: Boolean = false,
    ) = when (brevProsessType) {
        BrevProsessType.AUTOMATISK -> brevKodeAutomatisk(generellBrevData, erOmregningNyRegel)
        BrevProsessType.REDIGERBAR -> brevKodeAutomatisk(generellBrevData, erOmregningNyRegel)
        BrevProsessType.MANUELL -> BrevkodePar(EtterlatteBrevKode.OMS_OPPHOER_MANUELL)
    }

    private fun brevKodeAutomatisk(
        generellBrevData: GenerellBrevData,
        erOmregningNyRegel: Boolean = false,
    ): BrevkodePar {
        if (generellBrevData.systemkilde == Vedtaksloesning.PESYS || erOmregningNyRegel) {
            assert(listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING).contains(generellBrevData.forenkletVedtak?.type))
            return BrevkodePar(
                EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING,
                EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
            )
        }

        return when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (val vedtakType = generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE ->
                        when (brukNyInnvilgelsesmal()) {
                            true ->
                                BrevkodePar(
                                    EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_ENKEL,
                                    EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_NY,
                                )
                            false -> BrevkodePar(EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE)
                        }

                    VedtakType.AVSLAG ->
                        BrevkodePar(
                            EtterlatteBrevKode.BARNEPENSJON_AVSLAG_ENKEL,
                            EtterlatteBrevKode.BARNEPENSJON_AVSLAG,
                        )

                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SOESKENJUSTERING -> BrevkodePar(EtterlatteBrevKode.BARNEPENSJON_REVURDERING_SOESKENJUSTERING)
                            Revurderingaarsak.INSTITUSJONSOPPHOLD ->
                                BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            Revurderingaarsak.YRKESSKADE ->
                                BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            Revurderingaarsak.ANNEN ->
                                BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.ADOPSJON ->
                                BrevkodePar(
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ADOPSJON,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OPPHOER,
                                )

                            Revurderingaarsak.OMGJOERING_AV_FARSKAP ->
                                BrevkodePar(
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OPPHOER,
                                )

                            Revurderingaarsak.FENGSELSOPPHOLD ->
                                BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            Revurderingaarsak.UT_AV_FENGSEL ->
                                BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING,
                                )

                            else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                        }

                    VedtakType.TILBAKEKREVING ->
                        BrevkodePar(
                            EtterlatteBrevKode.TILBAKEKREVING_INNHOLD,
                            EtterlatteBrevKode.TILBAKEKREVING_FERDIG,
                        )
                    null ->
                        BrevkodePar(
                            EtterlatteBrevKode.TOM_DELMAL,
                            EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
                        )
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (val vedtakType = generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE ->
                        BrevkodePar(
                            EtterlatteBrevKode.OMS_FOERSTEGANGSVEDTAK_INNVILGELSE_UTFALL,
                            EtterlatteBrevKode.OMS_FOERSTEGANGSVEDTAK_INNVILGELSE,
                        )

                    VedtakType.AVSLAG ->
                        BrevkodePar(
                            EtterlatteBrevKode.OMS_AVSLAG_BEGRUNNELSE,
                            EtterlatteBrevKode.OMS_AVSLAG,
                        )
                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.INNTEKTSENDRING,
                            Revurderingaarsak.ANNEN,
                            ->
                                BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.OMS_REVURDERING_ENDRING,
                                )

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SIVILSTAND ->
                                BrevkodePar(
                                    EtterlatteBrevKode.OMS_REVURDERING_OPPHOER_GENERELL,
                                    EtterlatteBrevKode.OMS_REVURDERING_OPPHOER,
                                )

                            else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                        }

                    VedtakType.TILBAKEKREVING ->
                        BrevkodePar(
                            EtterlatteBrevKode.TILBAKEKREVING_INNHOLD,
                            EtterlatteBrevKode.TILBAKEKREVING_FERDIG,
                        )
                    null ->
                        BrevkodePar(
                            EtterlatteBrevKode.TOM_DELMAL,
                            EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV,
                        )
                }
            }
        }
    }

    private fun brukNyInnvilgelsesmal() = featureToggleService.isEnabled(BrevDataFeatureToggle.NyMalInnvilgelse, false)
}

data class BrevkodePar(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering) {
    fun erInformasjonsbrev() = ferdigstilling == EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV
}
