package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG_ENKEL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_ENKEL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_NY
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ADOPSJON
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_ENDRING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_SOESKENJUSTERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_AVSLAG_BEGRUNNELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_FOERSTEGANGSVEDTAK_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_FOERSTEGANGSVEDTAK_INNVILGELSE_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_OPPHOER_MANUELL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_REVURDERING_ENDRING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_REVURDERING_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMS_REVURDERING_OPPHOER_GENERELL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_INNHOLD
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.bp.AdopsjonRevurderingBrevdata
import no.nav.etterlatte.brev.model.bp.EndringHovedmalBrevData
import no.nav.etterlatte.brev.model.bp.InnvilgetBrevData
import no.nav.etterlatte.brev.model.bp.InnvilgetBrevDataEnkel
import no.nav.etterlatte.brev.model.bp.InnvilgetHovedmalBrevData
import no.nav.etterlatte.brev.model.bp.OmgjoeringAvFarskapRevurderingBrevdata
import no.nav.etterlatte.brev.model.bp.SoeskenjusteringRevurderingBrevdata
import no.nav.etterlatte.brev.model.oms.AvslagBrevDataOMS
import no.nav.etterlatte.brev.model.oms.FoerstegangsvedtakUtfallDTO
import no.nav.etterlatte.brev.model.oms.InntektsendringRevurderingOMS
import no.nav.etterlatte.brev.model.oms.InnvilgetBrevDataOMS
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingFerdigData
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingInnholdBrevData
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
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

    suspend fun hentForrigeUtbetaling() =
        brevdataFacade.finnForrigeUtbetalingsinfo(
            sak.id,
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

class BrevDataMapper(
    private val featureToggleService: FeatureToggleService,
    private val brevdataFacade: BrevdataFacade,
    private val migreringBrevDataService: MigreringBrevDataService,
) {
    fun brevKode(
        generellBrevData: GenerellBrevData,
        brevProsessType: BrevProsessType,
        erOmregningNyRegel: Boolean = false,
    ) = when (brevProsessType) {
        BrevProsessType.AUTOMATISK -> brevKodeAutomatisk(generellBrevData, erOmregningNyRegel)
        BrevProsessType.REDIGERBAR -> brevKodeAutomatisk(generellBrevData, erOmregningNyRegel)
        BrevProsessType.MANUELL -> BrevkodePar(OMS_OPPHOER_MANUELL)
    }

    private fun brevKodeAutomatisk(
        generellBrevData: GenerellBrevData,
        erOmregningNyRegel: Boolean = false,
    ): BrevkodePar {
        if (generellBrevData.systemkilde == Vedtaksloesning.PESYS || erOmregningNyRegel) {
            assert(listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING).contains(generellBrevData.forenkletVedtak.type))
            return BrevkodePar(BARNEPENSJON_VEDTAK_OMREGNING, BARNEPENSJON_VEDTAK_OMREGNING_FERDIG)
        }

        return when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE ->
                        when (brukNyInnvilgelsesmal()) {
                            true -> BrevkodePar(BARNEPENSJON_INNVILGELSE_ENKEL, BARNEPENSJON_INNVILGELSE_NY)
                            false -> BrevkodePar(BARNEPENSJON_INNVILGELSE)
                        }

                    VedtakType.AVSLAG -> BrevkodePar(BARNEPENSJON_AVSLAG_ENKEL, BARNEPENSJON_AVSLAG)

                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SOESKENJUSTERING -> BrevkodePar(BARNEPENSJON_REVURDERING_SOESKENJUSTERING)
                            Revurderingaarsak.INSTITUSJONSOPPHOLD ->
                                BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)

                            Revurderingaarsak.YRKESSKADE ->
                                BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)

                            Revurderingaarsak.ANNEN -> BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.ADOPSJON ->
                                BrevkodePar(BARNEPENSJON_REVURDERING_ADOPSJON, BARNEPENSJON_REVURDERING_OPPHOER)

                            Revurderingaarsak.OMGJOERING_AV_FARSKAP ->
                                BrevkodePar(
                                    BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP,
                                    BARNEPENSJON_REVURDERING_OPPHOER,
                                )

                            Revurderingaarsak.FENGSELSOPPHOLD ->
                                BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)

                            Revurderingaarsak.UT_AV_FENGSEL ->
                                BrevkodePar(TOM_MAL, BARNEPENSJON_REVURDERING_ENDRING)

                            else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                        }

                    VedtakType.TILBAKEKREVING -> BrevkodePar(TILBAKEKREVING_INNHOLD, TILBAKEKREVING_FERDIG)
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE ->
                        BrevkodePar(
                            OMS_FOERSTEGANGSVEDTAK_INNVILGELSE_UTFALL,
                            OMS_FOERSTEGANGSVEDTAK_INNVILGELSE,
                        )

                    VedtakType.AVSLAG -> BrevkodePar(OMS_AVSLAG_BEGRUNNELSE, OMS_AVSLAG)
                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.INNTEKTSENDRING,
                            Revurderingaarsak.ANNEN,
                            ->
                                BrevkodePar(TOM_MAL, OMS_REVURDERING_ENDRING)

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SIVILSTAND ->
                                BrevkodePar(OMS_REVURDERING_OPPHOER_GENERELL, OMS_REVURDERING_OPPHOER)

                            else -> TODO("Vedtakstype er ikke støttet: $vedtakType")
                        }

                    VedtakType.TILBAKEKREVING -> BrevkodePar(TILBAKEKREVING_INNHOLD, TILBAKEKREVING_FERDIG)
                }
            }
        }
    }

    suspend fun brevData(redigerbarTekstRequest: RedigerbarTekstRequest) =
        when (
            redigerbarTekstRequest.generellBrevData.systemkilde == Vedtaksloesning.PESYS ||
                redigerbarTekstRequest.migrering?.erOmregningGjenny ?: false
        ) {
            false ->
                brevData(
                    redigerbarTekstRequest.generellBrevData,
                    redigerbarTekstRequest.brukerTokenInfo,
                )
            true ->
                migreringBrevDataService.opprettMigreringBrevdata(
                    redigerbarTekstRequest.generellBrevData,
                    redigerbarTekstRequest.migrering,
                    redigerbarTekstRequest.brukerTokenInfo,
                )
        }

    suspend fun brevData(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevData {
        return when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE ->
                        when (brukNyInnvilgelsesmal()) {
                            true -> {
                                coroutineScope {
                                    val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                                    val utbetaling = async { fetcher.hentUtbetaling() }
                                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                                    InnvilgetBrevDataEnkel.fra(
                                        generellBrevData,
                                        utbetaling.await(),
                                        etterbetaling.await(),
                                    )
                                }
                            }

                            false -> {
                                coroutineScope {
                                    val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                                    val utbetaling =
                                        async { fetcher.hentUtbetaling() }
                                    val avkortinsinfo =
                                        async { fetcher.hentAvkortinginfo() }
                                    InnvilgetBrevData.fra(generellBrevData, utbetaling.await(), avkortinsinfo.await())
                                }
                            }
                        }

                    VedtakType.AVSLAG -> ManueltBrevData.fra()

                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SOESKENJUSTERING -> {
                                coroutineScope {
                                    val utbetalingsinfo =
                                        async {
                                            datafetcher(brukerTokenInfo, generellBrevData).hentUtbetaling()
                                        }
                                    SoeskenjusteringRevurderingBrevdata.fra(generellBrevData, utbetalingsinfo.await())
                                }
                            }

                            Revurderingaarsak.FENGSELSOPPHOLD,
                            Revurderingaarsak.UT_AV_FENGSEL,
                            Revurderingaarsak.INSTITUSJONSOPPHOLD,
                            Revurderingaarsak.YRKESSKADE,
                            Revurderingaarsak.ANNEN,
                            -> ManueltBrevData.fra()
                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.ADOPSJON ->
                                AdopsjonRevurderingBrevdata.fra(
                                    generellBrevData,
                                    LocalDate.now(),
                                ) // TODO: Denne må vi hente anten frå PDL eller brukarinput
                            Revurderingaarsak.OMGJOERING_AV_FARSKAP -> {
                                coroutineScope {
                                    val innvilgelsesDato =
                                        async {
                                            datafetcher(brukerTokenInfo, generellBrevData).hentInnvilgelsesdato()
                                        }
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

                    VedtakType.TILBAKEKREVING -> TilbakekrevingInnholdBrevData.fra(generellBrevData)
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (val vedtakType = generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE -> {
                        coroutineScope {
                            val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                            val utbetaling = async { fetcher.hentUtbetaling() }
                            val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                            val avkortingHentet =
                                requireNotNull(
                                    avkortingsinfo.await(),
                                ) { "$vedtakType, ${generellBrevData.sak.sakType} må ha avkortingsinfo " }
                            FoerstegangsvedtakUtfallDTO.fra(generellBrevData, utbetaling.await(), avkortingHentet)
                        }
                    }

                    VedtakType.AVSLAG ->
                        AvslagBrevDataOMS.fra(generellBrevData.personerISak.avdoede.first().navn, emptyList())
                    VedtakType.ENDRING ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.INNTEKTSENDRING,
                            Revurderingaarsak.ANNEN,
                            -> ManueltBrevData(emptyList())

                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.OPPHOER ->
                        when (generellBrevData.revurderingsaarsak) {
                            Revurderingaarsak.SIVILSTAND -> ManueltBrevData(emptyList())
                            else -> TODO("Revurderingsbrev for ${generellBrevData.revurderingsaarsak} er ikke støttet")
                        }

                    VedtakType.TILBAKEKREVING -> TilbakekrevingInnholdBrevData.fra(generellBrevData)
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
        return when (kode.ferdigstilling) {
            BARNEPENSJON_REVURDERING_ENDRING -> {
                coroutineScope {
                    val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val forrigeUtbetaling = async { fetcher.hentForrigeUtbetaling() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val grunnbeloep = async { fetcher.hentGrunnbeloep() }
                    val trygdetidHentet = requireNotNull(trygdetid.await()) { "${kode.ferdigstilling} Må ha trygdetid" }
                    val grunnbeloepHentet =
                        requireNotNull(grunnbeloep.await()) { "${kode.ferdigstilling} Må ha grunnbeløp" }
                    EndringHovedmalBrevData.fra(
                        utbetaling.await(),
                        forrigeUtbetalingsinfo = forrigeUtbetaling.await(),
                        etterbetaling.await(),
                        trygdetidHentet,
                        grunnbeloepHentet,
                        innholdMedVedlegg,
                    )
                }
            }

            BARNEPENSJON_INNVILGELSE_NY -> {
                coroutineScope {
                    val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val grunnbeloep = async { fetcher.hentGrunnbeloep() }
                    val trygdetidHentet = requireNotNull(trygdetid.await()) { "${kode.ferdigstilling} Må ha trygdetid" }
                    val grunnbeloepHentet =
                        requireNotNull(grunnbeloep.await()) { "${kode.ferdigstilling} Må ha grunnbeløp" }
                    InnvilgetHovedmalBrevData.fra(
                        utbetaling.await(),
                        avkortingsinfo.await(),
                        etterbetaling.await(),
                        trygdetidHentet,
                        grunnbeloepHentet,
                        generellBrevData.utlandstilknytning?.type,
                        innholdMedVedlegg,
                    )
                }
            }

            BARNEPENSJON_AVSLAG -> ManueltBrevData.fra(innholdMedVedlegg.innhold())

            OMS_FOERSTEGANGSVEDTAK_INNVILGELSE -> {
                coroutineScope {
                    val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val avkortingsinfoHentet =
                        requireNotNull(avkortingsinfo.await()) { "${kode.ferdigstilling} Må ha avkortingsinfo" }
                    val trygdetidHentet = requireNotNull(trygdetid.await()) { "${kode.ferdigstilling} Må ha trygdetid" }
                    InnvilgetBrevDataOMS.fra(
                        generellBrevData,
                        utbetaling.await(),
                        avkortingsinfoHentet,
                        etterbetaling.await(),
                        trygdetidHentet,
                        innholdMedVedlegg,
                    )
                }
            }

            OMS_REVURDERING_ENDRING -> {
                coroutineScope {
                    val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val avkortingsinfoHentet =
                        requireNotNull(avkortingsinfo.await()) { "${kode.ferdigstilling} Må ha avkortingsinfo" }
                    val trygdetidHentet = requireNotNull(trygdetid.await()) { "${kode.ferdigstilling} Må ha trygdetid" }
                    InntektsendringRevurderingOMS.fra(
                        avkortingsinfoHentet,
                        etterbetaling.await(),
                        trygdetidHentet,
                        innholdMedVedlegg,
                    )
                }
            }

            OMS_AVSLAG -> {
                AvslagBrevDataOMS.fra(generellBrevData.personerISak.avdoede.first().navn, innholdMedVedlegg.innhold())
            }

            TILBAKEKREVING_FERDIG -> TilbakekrevingFerdigData.fra(generellBrevData, innholdMedVedlegg)

            else ->
                when (generellBrevData.revurderingsaarsak?.redigerbartBrev) {
                    true -> ManueltBrevData(innholdMedVedlegg.innhold())
                    else -> brevData(generellBrevData, brukerTokenInfo)
                }
        }
    }

    private fun datafetcher(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = BrevDatafetcher(
        brevdataFacade,
        brukerTokenInfo,
        generellBrevData.behandlingId,
        requireNotNull(generellBrevData.forenkletVedtak.virkningstidspunkt) {
            "brev for behandling=${generellBrevData.behandlingId} må ha virkningstidspunkt"
        },
        generellBrevData.forenkletVedtak.type,
        generellBrevData.sak,
    )

    data class BrevkodePar(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering)

    private fun brukNyInnvilgelsesmal() = featureToggleService.isEnabled(BrevDataFeatureToggle.NyMalInnvilgelse, false)
}
