package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG_IKKEYRKESSKADE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_ENKEL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_NY
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ADOPSJON
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_SOESKENJUSTERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_FOERSTEGANGSVEDTAK_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_FOERSTEGANGSVEDTAK_INNVILGELSE_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_OPPHOER_MANUELL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_REVURDERING_ENDRING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_REVURDERING_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_REVURDERING_OPPHOER_GENERELL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.bp.AdopsjonRevurderingBrevdata
import no.nav.etterlatte.brev.model.bp.AvslagYrkesskadeBrevData
import no.nav.etterlatte.brev.model.bp.EndringHovedmalBrevData
import no.nav.etterlatte.brev.model.bp.InnvilgetBrevData
import no.nav.etterlatte.brev.model.bp.InnvilgetBrevDataEnkel
import no.nav.etterlatte.brev.model.bp.InnvilgetHovedmalBrevData
import no.nav.etterlatte.brev.model.bp.OmgjoeringAvFarskapRevurderingBrevdata
import no.nav.etterlatte.brev.model.bp.SoeskenjusteringRevurderingBrevdata
import no.nav.etterlatte.brev.model.oms.FoerstegangsvedtakUtfallDTO
import no.nav.etterlatte.brev.model.oms.InntektsendringRevurderingOMS
import no.nav.etterlatte.brev.model.oms.InnvilgetBrevDataOMS
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.BrukerTokenInfo
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class BrevDataFeatureToggle(private val key: String) : FeatureToggle {
    NyMalInnvilgelse("pensjon-etterlatte.bp-ny-mal-innvilgelse"),
    ;

    override fun key() = key
}

private class BrevDatafetcher(
    private val brevdataFacade: BrevdataFacade,
    private val brukerTokenInfo: BrukerTokenInfo,
    private val behandlingId: UUID,
    private val vedtakVirkningstidspunkt: YearMonth,
    private val type: VedtakType,
    private val sak: Sak,
) {
    suspend fun hentUtbetaling() =
        brevdataFacade.finnUtbetalingsinfo(
            behandlingId,
            vedtakVirkningstidspunkt,
            brukerTokenInfo,
        )

    suspend fun hentGrunnbeloep() = brevdataFacade.hentGrunnbeloep(brukerTokenInfo)

    suspend fun hentEtterbetaling() =
        brevdataFacade.hentEtterbetaling(
            behandlingId,
            brukerTokenInfo,
        )

    suspend fun hentAvkortinginfo() =
        brevdataFacade.finnAvkortingsinfo(
            behandlingId,
            sak.sakType,
            vedtakVirkningstidspunkt,
            type,
            brukerTokenInfo,
        )

    suspend fun hentInnvilgelsesdato() = brevdataFacade.hentInnvilgelsesdato(sak.id, brukerTokenInfo)

    suspend fun hentTrygdetid() = brevdataFacade.finnTrygdetid(behandlingId, brukerTokenInfo)
}

class BrevDataMapper(private val featureToggleService: FeatureToggleService, private val brevdataFacade: BrevdataFacade) {
    fun brevKode(
        generellBrevData: GenerellBrevData,
        brevProsessType: BrevProsessType,
    ) = when (brevProsessType) {
        BrevProsessType.AUTOMATISK -> brevKodeAutomatisk(generellBrevData)
        BrevProsessType.REDIGERBAR -> brevKodeAutomatisk(generellBrevData)
        BrevProsessType.MANUELL -> BrevkodePar(OMS_OPPHOER_MANUELL)
    }

    private fun brevKodeAutomatisk(generellBrevData: GenerellBrevData): BrevkodePar =
        when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE ->
                        when (brukNyInnvilgelsesmal()) {
                            true -> BrevkodePar(BARNEPENSJON_INNVILGELSE_ENKEL, BARNEPENSJON_INNVILGELSE_NY)
                            false -> BrevkodePar(BARNEPENSJON_INNVILGELSE)
                        }

                    VedtakType.AVSLAG ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.YRKESSKADE -> BrevkodePar(BARNEPENSJON_AVSLAG_IKKEYRKESSKADE, BARNEPENSJON_AVSLAG)
                            else -> BrevkodePar(BARNEPENSJON_AVSLAG)
                        }

                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.SOESKENJUSTERING -> BrevkodePar(BARNEPENSJON_REVURDERING_SOESKENJUSTERING)
                            RevurderingAarsak.INSTITUSJONSOPPHOLD ->
                                BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)
                            RevurderingAarsak.YRKESSKADE ->
                                BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)
                            RevurderingAarsak.ANNEN -> BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.ADOPSJON ->
                                BrevkodePar(BARNEPENSJON_REVURDERING_ADOPSJON, BARNEPENSJON_REVURDERING_OPPHOER)

                            RevurderingAarsak.OMGJOERING_AV_FARSKAP ->
                                BrevkodePar(BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP, BARNEPENSJON_REVURDERING_OPPHOER)

                            RevurderingAarsak.FENGSELSOPPHOLD ->
                                BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)

                            RevurderingAarsak.UT_AV_FENGSEL ->
                                BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)

                            else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                        }
                    VedtakType.TILBAKEKREVING -> TODO("EY-2806")
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE ->
                        BrevkodePar(
                            OMS_FOERSTEGANGSVEDTAK_INNVILGELSE_UTFALL,
                            OMS_FOERSTEGANGSVEDTAK_INNVILGELSE,
                        )
                    VedtakType.AVSLAG -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.INNTEKTSENDRING,
                            RevurderingAarsak.ANNEN,
                            ->
                                BrevkodePar(
                                    TOM_MAL,
                                    OMS_REVURDERING_ENDRING,
                                )
                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }
                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.SIVILSTAND ->
                                BrevkodePar(OMS_REVURDERING_OPPHOER_GENERELL, OMS_REVURDERING_OPPHOER)
                            else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                        }
                    VedtakType.TILBAKEKREVING -> TODO("EY-2806")
                }
            }
        }

    suspend fun brevData(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevData {
        val fetcher =
            BrevDatafetcher(
                brevdataFacade,
                brukerTokenInfo,
                generellBrevData.behandlingId,
                generellBrevData.forenkletVedtak.virkningstidspunkt,
                generellBrevData.forenkletVedtak.type,
                generellBrevData.sak,
            )

        return when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE ->
                        when (brukNyInnvilgelsesmal()) {
                            true -> {
                                coroutineScope {
                                    val utbetaling =
                                        async { fetcher.hentUtbetaling() }
                                    InnvilgetBrevDataEnkel.fra(generellBrevData, utbetaling.await())
                                }
                            }
                            false -> {
                                coroutineScope {
                                    val utbetaling =
                                        async { fetcher.hentUtbetaling() }
                                    val avkortinsinfo =
                                        async { fetcher.hentAvkortinginfo() }
                                    InnvilgetBrevData.fra(generellBrevData, utbetaling.await(), avkortinsinfo.await())
                                }
                            }
                        }

                    VedtakType.AVSLAG ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.YRKESSKADE -> AvslagYrkesskadeBrevData.fra(generellBrevData)
                            else -> AvslagBrevData.fra()
                        }

                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.SOESKENJUSTERING -> {
                                coroutineScope {
                                    val utbetalingsinfo = async { fetcher.hentUtbetaling() }
                                    SoeskenjusteringRevurderingBrevdata.fra(generellBrevData, utbetalingsinfo.await())
                                }
                            }
                            RevurderingAarsak.FENGSELSOPPHOLD,
                            RevurderingAarsak.UT_AV_FENGSEL,
                            RevurderingAarsak.INSTITUSJONSOPPHOLD,
                            -> ManueltBrevData(emptyList())

                            RevurderingAarsak.YRKESSKADE -> ManueltBrevData(emptyList())
                            RevurderingAarsak.ANNEN -> ManueltBrevData(emptyList())
                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.ADOPSJON ->
                                AdopsjonRevurderingBrevdata.fra(
                                    generellBrevData,
                                    LocalDate.now(),
                                ) // TODO: Denne må vi hente anten frå PDL eller brukarinput
                            RevurderingAarsak.OMGJOERING_AV_FARSKAP -> {
                                coroutineScope {
                                    val innvilgelsesDato = async { fetcher.hentInnvilgelsesdato() }
                                    val innvilgelsesDatoHentet =
                                        requireNotNull(
                                            innvilgelsesDato.await(),
                                        ) { "${generellBrevData.revurderingsaarsak} må ha en innvigelsesdato fra vedtak type: $vedtakType" }
                                    OmgjoeringAvFarskapRevurderingBrevdata.fra(
                                        generellBrevData,
                                        innvilgelsesDatoHentet,
                                    )
                                }
                            }

                            else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                        }

                    VedtakType.TILBAKEKREVING -> TODO("EY-2806")
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE -> {
                        coroutineScope {
                            val utbetaling = async { fetcher.hentUtbetaling() }
                            val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                            val avkortingHentet =
                                requireNotNull(
                                    avkortingsinfo.await(),
                                ) { "$vedtakType, ${generellBrevData.sak.sakType} må ha avkortingsinfo " }
                            FoerstegangsvedtakUtfallDTO.fra(generellBrevData, utbetaling.await(), avkortingHentet)
                        }
                    }
                    VedtakType.AVSLAG -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.INNTEKTSENDRING,
                            RevurderingAarsak.ANNEN,
                            -> ManueltBrevData(emptyList())

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.SIVILSTAND -> ManueltBrevData(emptyList())
                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.TILBAKEKREVING -> TODO("EY-2806")
                }
            }
        }
    }

    suspend fun brevDataFerdigstilling(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        kode: BrevkodePar,
    ): BrevData {
        val fetcher =
            BrevDatafetcher(
                brevdataFacade,
                brukerTokenInfo,
                generellBrevData.behandlingId,
                generellBrevData.forenkletVedtak.virkningstidspunkt,
                generellBrevData.forenkletVedtak.type,
                generellBrevData.sak,
            )

        return when (kode.ferdigstilling) {
            BARNEPENSJON_REVURDERING_ENDRING -> {
                coroutineScope {
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    EndringHovedmalBrevData.fra(utbetaling.await(), etterbetaling.await(), innholdMedVedlegg)
                }
            }
            BARNEPENSJON_INNVILGELSE_NY -> {
                coroutineScope {
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val grunnbeloep = async { fetcher.hentGrunnbeloep() }
                    val avkortingsinfoHentet = requireNotNull(avkortingsinfo.await()) { "${kode.ferdigstilling} Må ha avkortingsinfo" }
                    val etterbetalingHentet = requireNotNull(etterbetaling.await()) { "${kode.ferdigstilling} Må ha etterbetalingsinfo" }
                    val trygdetidHentet = requireNotNull(trygdetid.await()) { "${kode.ferdigstilling} Må ha trygdetid" }
                    val grunnbeloepHentet = requireNotNull(grunnbeloep.await()) { "${kode.ferdigstilling} Må ha grunnbeløp" }
                    InnvilgetHovedmalBrevData.fra(
                        utbetaling.await(),
                        avkortingsinfoHentet,
                        etterbetalingHentet,
                        trygdetidHentet,
                        grunnbeloepHentet,
                        innholdMedVedlegg,
                    )
                }
            }
            OMS_FOERSTEGANGSVEDTAK_INNVILGELSE -> {
                coroutineScope {
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val avkortingsinfoHentet = requireNotNull(avkortingsinfo.await()) { "${kode.ferdigstilling} Må ha avkortingsinfo" }
                    val etterbetalingHentet = requireNotNull(etterbetaling.await()) { "${kode.ferdigstilling} Må ha etterbetalingsinfo" }
                    val trygdetidHentet = requireNotNull(trygdetid.await()) { "${kode.ferdigstilling} Må ha trygdetid" }
                    InnvilgetBrevDataOMS.fra(
                        generellBrevData,
                        utbetaling.await(),
                        avkortingsinfoHentet,
                        etterbetalingHentet,
                        trygdetidHentet,
                        innholdMedVedlegg,
                    )
                }
            }

            OMS_REVURDERING_ENDRING -> {
                coroutineScope {
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val avkortingsinfoHentet = requireNotNull(avkortingsinfo.await()) { "${kode.ferdigstilling} Må ha avkortingsinfo" }
                    val etterbetalingHentet = requireNotNull(etterbetaling.await()) { "${kode.ferdigstilling} Må ha etterbetalingsinfo" }
                    val trygdetidHentet = requireNotNull(trygdetid.await()) { "${kode.ferdigstilling} Må ha trygdetid" }
                    InntektsendringRevurderingOMS.fra(
                        avkortingsinfoHentet,
                        etterbetalingHentet,
                        trygdetidHentet,
                        innholdMedVedlegg,
                    )
                }
            }
            else ->
                when (generellBrevData.revurderingsaarsak?.redigerbartBrev) {
                    true -> ManueltBrevData(innholdMedVedlegg.innhold())
                    else -> brevData(generellBrevData, brukerTokenInfo)
                }
        }
    }

    data class BrevkodePar(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering)

    private fun brukNyInnvilgelsesmal() = featureToggleService.isEnabled(BrevDataFeatureToggle.NyMalInnvilgelse, false)
}
