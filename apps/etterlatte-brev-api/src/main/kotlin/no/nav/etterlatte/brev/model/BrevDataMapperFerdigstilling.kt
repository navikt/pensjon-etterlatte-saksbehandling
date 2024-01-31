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
import no.nav.etterlatte.brev.model.bp.BarnepensjonAvslag
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelse
import no.nav.etterlatte.brev.model.bp.BarnepensjonOmregnetNyttRegelverk
import no.nav.etterlatte.brev.model.bp.BarnepensjonOpphoer
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurdering
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAvslag
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelse
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadOpphoer
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurdering
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingFerdigData
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
                val utbetalingsinfo = async { fetcher.hentUtbetaling() }
                val trygdetid = async { fetcher.hentTrygdetid() }
                val grunnbeloep = async { fetcher.hentGrunnbeloep() }
                val etterbetaling = async { fetcher.hentEtterbetaling() }

                BarnepensjonOmregnetNyttRegelverk.fra(
                    innhold = innholdMedVedlegg,
                    erUnder18Aar = generellBrevData.personerISak.soeker.under18,
                    utbetalingsinfo = utbetalingsinfo.await(),
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
                    val utbetalingsinfo = async { fetcher.hentUtbetaling() }
                    val forrigeUtbetalingsinfo = async { fetcher.hentForrigeUtbetaling() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val grunnbeloep = async { fetcher.hentGrunnbeloep() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val brevutfall = async { fetcher.hentBrevutfall() }

                    BarnepensjonRevurdering.fra(
                        innholdMedVedlegg,
                        utbetalingsinfo.await(),
                        forrigeUtbetalingsinfo.await(),
                        etterbetaling.await(),
                        requireNotNull(trygdetid.await()),
                        requireNotNull(grunnbeloep.await()),
                        generellBrevData.utlandstilknytning?.type,
                        requireNotNull(brevutfall.await()),
                    )
                }
            }

            BARNEPENSJON_INNVILGELSE -> {
                coroutineScope {
                    val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
                    val utbetalingsinfo = async { fetcher.hentUtbetaling() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val grunnbeloep = async { fetcher.hentGrunnbeloep() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val brevutfall = async { fetcher.hentBrevutfall() }

                    BarnepensjonInnvilgelse.fra(
                        innholdMedVedlegg,
                        utbetalingsinfo.await(),
                        etterbetaling.await(),
                        requireNotNull(trygdetid.await()),
                        requireNotNull(grunnbeloep.await()),
                        generellBrevData.utlandstilknytning?.type,
                        requireNotNull(brevutfall.await()),
                    )
                }
            }

            BARNEPENSJON_AVSLAG -> {
                BarnepensjonAvslag.fra(
                    innhold = innholdMedVedlegg,
                    // TODO må kunne sette brevutfall ved avslag.
                    //  Det er pr nå ikke mulig da dette ligger i beregningssteget.
                    brukerUnder18Aar = generellBrevData.personerISak.soeker.under18 ?: true,
                    utlandstilknytning = generellBrevData.utlandstilknytning?.type,
                )
            }

            BARNEPENSJON_OPPHOER -> {
                BarnepensjonOpphoer.fra(
                    innhold = innholdMedVedlegg,
                    // TODO må kunne sette brevutfall ved opphør.
                    //  Det er pr nå ikke mulig da dette ligger i beregningssteget.
                    brukerUnder18Aar = generellBrevData.personerISak.soeker.under18 ?: true,
                    utlandstilknytning = generellBrevData.utlandstilknytning?.type,
                )
            }

            OMSTILLINGSSTOENAD_INNVILGELSE -> {
                coroutineScope {
                    val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
                    val utbetalingsinfo = async { fetcher.hentUtbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }
                    val brevutfall = async { fetcher.hentBrevutfall() }

                    OmstillingsstoenadInnvilgelse.fra(
                        innholdMedVedlegg,
                        generellBrevData,
                        utbetalingsinfo.await(),
                        requireNotNull(avkortingsinfo.await()),
                        etterbetaling.await(),
                        requireNotNull(trygdetid.await()),
                        requireNotNull(brevutfall.await()),
                    )
                }
            }

            OMSTILLINGSSTOENAD_REVURDERING -> {
                coroutineScope {
                    val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
                    val utbetalingsinfo = async { fetcher.hentUtbetaling() }
                    val forrigeUtbetalingsinfo = async { fetcher.hentForrigeUtbetaling() }
                    val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                    val trygdetid = async { fetcher.hentTrygdetid() }
                    val etterbetaling = async { fetcher.hentEtterbetaling() }

                    OmstillingsstoenadRevurdering.fra(
                        innholdMedVedlegg,
                        requireNotNull(avkortingsinfo.await()),
                        utbetalingsinfo.await(),
                        forrigeUtbetalingsinfo.await(),
                        etterbetaling.await(),
                        requireNotNull(trygdetid.await()),
                    )
                }
            }

            OMSTILLINGSSTOENAD_AVSLAG -> OmstillingsstoenadAvslag.fra(generellBrevData, innholdMedVedlegg.innhold())
            OMSTILLINGSSTOENAD_OPPHOER -> {
                OmstillingsstoenadOpphoer.fra(
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
