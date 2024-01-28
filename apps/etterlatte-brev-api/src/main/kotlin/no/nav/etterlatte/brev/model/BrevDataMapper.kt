package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.bp.AvslagBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelseDTO
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelseRedigerbartUtfallDTO
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurderingDTO
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurderingRedigerbartUtfallDTO
import no.nav.etterlatte.brev.model.bp.OpphoerBrevData
import no.nav.etterlatte.brev.model.oms.AvslagBrevDataOMS
import no.nav.etterlatte.brev.model.oms.InntektsendringRevurderingOMS
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseDTO
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseRedigerbartUtfallDTO
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingFerdigData
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingInnholdBrevData
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.LavEllerIngenInntekt
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.BrukerTokenInfo
import java.time.YearMonth
import java.util.UUID

private class BrevDatafetcher(
    private val brevdataFacade: BrevdataFacade,
    private val brukerTokenInfo: BrukerTokenInfo,
    private val behandlingId: UUID?,
    private val vedtakVirkningstidspunkt: YearMonth,
    private val type: VedtakType?,
    private val sak: Sak,
) {
    suspend fun hentBrevutfall() = behandlingId?.let { brevdataFacade.hentBrevutfall(it, brukerTokenInfo) }

    suspend fun hentUtbetaling() =
        brevdataFacade.finnUtbetalingsinfo(
            behandlingId!!,
            vedtakVirkningstidspunkt,
            brukerTokenInfo,
            sak.sakType,
        )

    suspend fun hentForrigeUtbetaling() =
        brevdataFacade.finnForrigeUtbetalingsinfo(
            sak.id,
            vedtakVirkningstidspunkt,
            brukerTokenInfo,
            sak.sakType,
        )

    suspend fun hentGrunnbeloep() = brevdataFacade.hentGrunnbeloep(brukerTokenInfo)

    suspend fun hentEtterbetaling() =
        behandlingId?.let {
            brevdataFacade.hentEtterbetaling(
                it,
                brukerTokenInfo,
            )
        }

    suspend fun hentAvkortinginfo() =
        behandlingId?.let {
            brevdataFacade.finnAvkortingsinfo(
                it,
                sak.sakType,
                vedtakVirkningstidspunkt,
                type!!,
                brukerTokenInfo,
            )
        }

    suspend fun hentTrygdetid() = behandlingId?.let { brevdataFacade.finnTrygdetid(it, brukerTokenInfo) }
}

class BrevDataMapper(
    private val brevdataFacade: BrevdataFacade,
    private val migreringBrevDataService: MigreringBrevDataService,
) {
    suspend fun brevData(redigerbarTekstRequest: RedigerbarTekstRequest) =
        when (redigerbarTekstRequest.generellBrevData.erMigrering()) {
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
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE ->
                        coroutineScope {
                            val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                            val utbetaling = async { fetcher.hentUtbetaling() }
                            val etterbetaling = async { fetcher.hentEtterbetaling() }
                            BarnepensjonInnvilgelseRedigerbartUtfallDTO.fra(
                                generellBrevData,
                                utbetaling.await(),
                                etterbetaling.await(),
                            )
                        }

                    VedtakType.AVSLAG -> ManueltBrevData.fra()

                    VedtakType.ENDRING ->
                        coroutineScope {
                            val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                            val etterbetaling = async { fetcher.hentEtterbetaling() }
                            BarnepensjonRevurderingRedigerbartUtfallDTO.fra(etterbetaling.await())
                        }
                    VedtakType.OPPHOER -> ManueltBrevData.fra()
                    VedtakType.TILBAKEKREVING -> TilbakekrevingInnholdBrevData.fra(generellBrevData)
                    null -> ManueltBrevData.fra(emptyList())
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (val vedtakType = generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> {
                        coroutineScope {
                            val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                            val utbetaling = async { fetcher.hentUtbetaling() }
                            val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                            val etterbetaling = async { fetcher.hentEtterbetaling() }
                            val avkortingHentet =
                                requireNotNull(
                                    avkortingsinfo.await(),
                                ) { "$vedtakType, ${generellBrevData.sak.sakType} må ha avkortingsinfo " }
                            OmstillingsstoenadInnvilgelseRedigerbartUtfallDTO.fra(
                                generellBrevData,
                                utbetaling.await(),
                                avkortingHentet,
                                etterbetaling.await() != null,
                            )
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
                    null -> ManueltBrevData.fra(emptyList())
                }
            }
        }
    }

    suspend fun brevDataFerdigstilling(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        kode: Brevkoder,
        tittel: String? = null,
    ): BrevData {
        return when (kode.ferdigstilling) {
            TOM_MAL_INFORMASJONSBREV -> {
                ManueltBrevMedTittelData(innholdMedVedlegg.innhold(), tittel)
            }
            BARNEPENSJON_REVURDERING -> {
                coroutineScope {
                    val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val forrigeUtbetaling = async { fetcher.hentForrigeUtbetaling() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val grunnbeloep = async { fetcher.hentGrunnbeloep() }
                    val brevutfall = async { fetcher.hentBrevutfall() }
                    val trygdetidHentet =
                        requireNotNull(
                            trygdetid.await(),
                        ) { "${kode.ferdigstilling} Har ikke trygdetid, det er påbudt for ${BARNEPENSJON_INNVILGELSE.name}" }
                    val grunnbeloepHentet =
                        requireNotNull(grunnbeloep.await()) { "${kode.ferdigstilling} Må ha grunnbeløp" }
                    val brevutfallHentet =
                        requireNotNull(brevutfall.await()) {
                            "${kode.ferdigstilling} Må ha brevutfall for å avgjøre over eller under 18 år"
                        }
                    BarnepensjonRevurderingDTO.fra(
                        innholdMedVedlegg,
                        utbetaling.await(),
                        forrigeUtbetaling.await(),
                        etterbetaling.await(),
                        trygdetidHentet,
                        grunnbeloepHentet,
                        generellBrevData.utlandstilknytning?.type,
                        brevutfallHentet.aldersgruppe == Aldersgruppe.UNDER_18,
                    )
                }
            }

            BARNEPENSJON_INNVILGELSE -> {
                coroutineScope {
                    val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val grunnbeloep = async { fetcher.hentGrunnbeloep() }
                    val brevutfall = async { fetcher.hentBrevutfall() }
                    val trygdetidHentet =
                        requireNotNull(
                            trygdetid.await(),
                        ) { "${kode.ferdigstilling} Har ikke trygdetid, det er påbudt for ${BARNEPENSJON_INNVILGELSE.name}" }
                    val grunnbeloepHentet =
                        requireNotNull(grunnbeloep.await()) { "${kode.ferdigstilling} Må ha grunnbeløp" }

                    val brevutfallHentet =
                        requireNotNull(brevutfall.await()) {
                            "${kode.ferdigstilling} Må ha brevutfall for å avgjøre over eller under 18 år"
                        }
                    BarnepensjonInnvilgelseDTO.fra(
                        utbetaling.await(),
                        etterbetaling.await(),
                        trygdetidHentet,
                        grunnbeloepHentet,
                        generellBrevData.utlandstilknytning?.type,
                        innholdMedVedlegg,
                        brevutfallHentet.aldersgruppe == Aldersgruppe.UNDER_18,
                    )
                }
            }

            BARNEPENSJON_AVSLAG -> {
                AvslagBrevData.fra(
                    innhold = innholdMedVedlegg,
                    // TODO må kunne sette brevutfall ved avslag. Det er pr nå ikke mulig da dette ligger i beregningssteget.
                    brukerUnder18Aar = generellBrevData.personerISak.soeker.under18 ?: true,
                    utlandstilknytning = generellBrevData.utlandstilknytning?.type,
                )
            }

            BARNEPENSJON_OPPHOER -> {
                OpphoerBrevData.fra(
                    innhold = innholdMedVedlegg,
                    // TODO må kunne sette brevutfall ved opphør. Det er pr nå ikke mulig da dette ligger i beregningssteget.
                    brukerUnder18Aar = generellBrevData.personerISak.soeker.under18 ?: true,
                    utlandstilknytning = generellBrevData.utlandstilknytning?.type,
                )
            }

            OMSTILLINGSSTOENAD_INNVILGELSE -> {
                coroutineScope {
                    val fetcher = datafetcher(brukerTokenInfo, generellBrevData)
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val brevutfall = async { fetcher.hentBrevutfall() }
                    val avkortingsinfoHentet =
                        requireNotNull(avkortingsinfo.await()) { "${kode.ferdigstilling} Må ha avkortingsinfo" }
                    val trygdetidHentet = requireNotNull(trygdetid.await()) { "${kode.ferdigstilling} Må ha trygdetid" }
                    val brevutfallHentet =
                        requireNotNull(brevutfall.await()) {
                            "${kode.ferdigstilling} Må ha brevutfall for å avgjøre lav eller ingen inntekt"
                        }
                    OmstillingsstoenadInnvilgelseDTO.fra(
                        generellBrevData,
                        utbetaling.await(),
                        avkortingsinfoHentet,
                        etterbetaling.await(),
                        trygdetidHentet,
                        innholdMedVedlegg,
                        brevutfallHentet.lavEllerIngenInntekt == LavEllerIngenInntekt.JA,
                    )
                }
            }

            OMSTILLINGSSTOENAD_REVURDERING -> {
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

            OMSTILLINGSSTOENAD_AVSLAG -> {
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
        requireNotNull(generellBrevData.forenkletVedtak?.virkningstidspunkt) {
            "brev for behandling=${generellBrevData.behandlingId} må ha virkningstidspunkt"
        },
        generellBrevData.forenkletVedtak?.type,
        generellBrevData.sak,
    )
}
