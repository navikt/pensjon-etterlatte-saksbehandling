package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevKodeMapper {
    fun brevKode(
        generellBrevData: GenerellBrevData,
        brevProsessType: BrevProsessType,
        erOmregningNyRegel: Boolean = false,
    ) = when (brevProsessType) {
        BrevProsessType.AUTOMATISK -> brevKodeAutomatisk(generellBrevData, erOmregningNyRegel)
        BrevProsessType.REDIGERBAR -> brevKodeAutomatisk(generellBrevData, erOmregningNyRegel)
        BrevProsessType.MANUELL -> BrevkodePar(EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_MANUELL)
        BrevProsessType.OPPLASTET_PDF -> throw IllegalStateException("Brevkode ikke relevant for ${BrevProsessType.OPPLASTET_PDF}")
    }

    private fun brevKodeAutomatisk(
        generellBrevData: GenerellBrevData,
        erOmregningNyRegel: Boolean = false,
    ): BrevkodePar {
        if (generellBrevData.erMigrering() || erOmregningNyRegel) {
            assert(listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING).contains(generellBrevData.forenkletVedtak?.type))
            return BrevkodePar(
                EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING,
                EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
            )
        }

        return when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE ->
                        BrevkodePar(
                            EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL,
                            EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE,
                        )

                    VedtakType.AVSLAG ->
                        BrevkodePar(
                            EtterlatteBrevKode.BARNEPENSJON_AVSLAG_UTFALL,
                            EtterlatteBrevKode.BARNEPENSJON_AVSLAG,
                        )

                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SOESKENJUSTERING -> BrevkodePar(EtterlatteBrevKode.BARNEPENSJON_REVURDERING_SOESKENJUSTERING)
                            else ->
                                BrevkodePar(
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING_UTFALL,
                                    EtterlatteBrevKode.BARNEPENSJON_REVURDERING,
                                )
                        }

                    VedtakType.OPPHOER ->
                        BrevkodePar(
                            EtterlatteBrevKode.BARNEPENSJON_OPPHOER_UTFALL,
                            EtterlatteBrevKode.BARNEPENSJON_OPPHOER,
                        )

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
                            EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
                            EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE,
                        )

                    VedtakType.AVSLAG ->
                        BrevkodePar(
                            EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
                            EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG,
                        )
                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.INNTEKTSENDRING,
                            Revurderingaarsak.ANNEN,
                            ->
                                BrevkodePar(
                                    EtterlatteBrevKode.TOM_MAL,
                                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING,
                                )

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SIVILSTAND ->
                                BrevkodePar(
                                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_UTFALL,
                                    EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING_OPPHOER,
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
}

data class BrevkodePar(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering)
