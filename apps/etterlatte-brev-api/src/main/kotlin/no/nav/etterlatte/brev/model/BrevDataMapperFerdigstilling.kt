package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.bp.AvslagBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelseDTO
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurderingDTO
import no.nav.etterlatte.brev.model.bp.OmregnetBPNyttRegelverkFerdig
import no.nav.etterlatte.brev.model.bp.OpphoerBrevData
import no.nav.etterlatte.brev.model.oms.AvslagBrevDataOMS
import no.nav.etterlatte.brev.model.oms.InntektsendringRevurderingOMS
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseDTO
import no.nav.etterlatte.brev.model.oms.OpphoerBrevDataOMS
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingFerdigData
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.LavEllerIngenInntekt
import no.nav.etterlatte.token.BrukerTokenInfo

class BrevDataMapperFerdigstilling(
    private val brevdataFacade: BrevdataFacade,
    private val migreringBrevDataService: MigreringBrevDataService,
    private val brevDataMapper: BrevDataMapper, // TODO: Håper vi kan få bort denne koplinga snart
) {
    suspend fun brevDataFerdigstilling(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        kode: Brevkoder,
        automatiskMigreringRequest: MigreringBrevRequest?,
        tittel: String? = null,
    ): BrevData {
        if (generellBrevData.erMigrering()) {
            return coroutineScope {
                val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
                val utbetaling = async { fetcher.hentUtbetaling() }
                val etterbetaling = async { fetcher.hentEtterbetaling() }
                val trygdetid = async { fetcher.hentTrygdetid() }
                val grunnbeloep = async { fetcher.hentGrunnbeloep() }

                val erUnder18Aar =
                    requireNotNull(generellBrevData.personerISak.soeker.under18) {
                        "Klarte ikke å bestemme om alder på søker er under eller over 18 år. Kan dermed ikke velge riktig brev"
                    }
                OmregnetBPNyttRegelverkFerdig.fra(
                    innhold = innholdMedVedlegg,
                    erUnder18Aar = erUnder18Aar,
                    utbetalingsinfo = utbetaling.await(),
                    etterbetaling = etterbetaling.await(),
                    trygdetid = requireNotNull(trygdetid.await()),
                    grunnbeloep = grunnbeloep.await(),
                    migreringRequest = automatiskMigreringRequest,
                    utlandstilknytning = generellBrevData.utlandstilknytning?.type,
                )
            }
        }

        return when (kode.ferdigstilling) {
            TOM_MAL_INFORMASJONSBREV -> {
                ManueltBrevMedTittelData(innholdMedVedlegg.innhold(), tittel)
            }

            BARNEPENSJON_REVURDERING -> {
                coroutineScope {
                    val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
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
                    val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
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
                    val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
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
                    val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val utbetaling = async { fetcher.hentUtbetaling() }
                    val forrigeUtbetaling = async { fetcher.hentForrigeUtbetaling() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val avkortingsinfoHentet =
                        requireNotNull(avkortingsinfo.await()) { "${kode.ferdigstilling} Må ha avkortingsinfo" }
                    val trygdetidHentet = requireNotNull(trygdetid.await()) { "${kode.ferdigstilling} Må ha trygdetid" }
                    InntektsendringRevurderingOMS.fra(
                        avkortingsinfoHentet,
                        utbetaling.await(),
                        forrigeUtbetaling.await(),
                        etterbetaling.await(),
                        trygdetidHentet,
                        innholdMedVedlegg,
                    )
                }
            }

            OMSTILLINGSSTOENAD_AVSLAG -> {
                AvslagBrevDataOMS.fra(
                    generellBrevData.personerISak.avdoede.first().navn,
                    generellBrevData.utlandstilknytning,
                    innholdMedVedlegg.innhold(),
                )
            }

            OMSTILLINGSSTOENAD_OPPHOER -> {
                OpphoerBrevDataOMS.fra(
                    generellBrevData.utlandstilknytning,
                    innholdMedVedlegg.innhold(),
                )
            }

            TILBAKEKREVING_FERDIG -> TilbakekrevingFerdigData.fra(generellBrevData, innholdMedVedlegg)

            else ->
                when (generellBrevData.revurderingsaarsak?.redigerbartBrev) {
                    true -> ManueltBrevData(innholdMedVedlegg.innhold())
                    else -> brevData(generellBrevData, brukerTokenInfo)
                }
        }
    }

    suspend fun brevData(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
    ) = brevDataMapper.brevData(generellBrevData, brukerTokenInfo)
}
