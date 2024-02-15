package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.AVVIST_KLAGE_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_FORELDRELOES
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_FERDIG
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.bp.BarnepensjonAvslag
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelse
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelseForeldreloes
import no.nav.etterlatte.brev.model.bp.BarnepensjonOmregnetNyttRegelverk
import no.nav.etterlatte.brev.model.bp.BarnepensjonOpphoer
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurdering
import no.nav.etterlatte.brev.model.klage.AvvistKlageFerdigData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAvslag
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelse
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadOpphoer
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurdering
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingFerdigData
import no.nav.etterlatte.token.BrukerTokenInfo

data class BrevDataFerdigstillingRequest(
    val generellBrevData: GenerellBrevData,
    val bruker: BrukerTokenInfo,
    val innholdMedVedlegg: InnholdMedVedlegg,
    val kode: Brevkoder,
    val automatiskMigreringRequest: MigreringBrevRequest?,
    val tittel: String? = null,
)

class BrevDataMapperFerdigstillingVedtak(private val brevdataFacade: BrevdataFacade) {
    suspend fun brevDataFerdigstilling(request: BrevDataFerdigstillingRequest): BrevDataFerdigstilling {
        with(request) {
            if (generellBrevData.loependeIPesys()) {
                return fraPesys(bruker, generellBrevData, innholdMedVedlegg, automatiskMigreringRequest)
            }
            return when (kode.ferdigstilling) {
                BARNEPENSJON_REVURDERING -> barnepensjonRevurdering(bruker, generellBrevData, innholdMedVedlegg)
                BARNEPENSJON_INNVILGELSE,
                BARNEPENSJON_INNVILGELSE_FORELDRELOES,
                -> barnepensjonInnvilgelse(bruker, generellBrevData, innholdMedVedlegg)
                BARNEPENSJON_AVSLAG -> barnepensjonAvslag(innholdMedVedlegg, generellBrevData)
                BARNEPENSJON_OPPHOER -> barnepensjonOpphoer(bruker, innholdMedVedlegg, generellBrevData)

                OMSTILLINGSSTOENAD_INNVILGELSE ->
                    omstillingsstoenadInnvilgelse(
                        bruker,
                        generellBrevData,
                        innholdMedVedlegg,
                    )

                OMSTILLINGSSTOENAD_REVURDERING ->
                    omstillingsstoenadRevurdering(
                        bruker,
                        generellBrevData,
                        innholdMedVedlegg,
                    )

                OMSTILLINGSSTOENAD_AVSLAG -> OmstillingsstoenadAvslag.fra(generellBrevData, innholdMedVedlegg.innhold())
                OMSTILLINGSSTOENAD_OPPHOER -> omstillingsstoenadOpphoer(bruker, generellBrevData, innholdMedVedlegg)

                TILBAKEKREVING_FERDIG -> TilbakekrevingFerdigData.fra(generellBrevData, innholdMedVedlegg)

                AVVIST_KLAGE_FERDIG -> AvvistKlageFerdigData.fra(generellBrevData, innholdMedVedlegg)

                else -> throw IllegalStateException("Klarte ikke å finne brevdata for brevkode $kode for ferdigstilling.")
            }
        }
    }

    private suspend fun fraPesys(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
        automatiskMigreringRequest: MigreringBrevRequest?,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, bruker, generellBrevData)
        val utbetalingsinfo = async { fetcher.hentUtbetaling() }
        val trygdetid = async { fetcher.hentTrygdetid() }
        val grunnbeloep = async { fetcher.hentGrunnbeloep() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }

        if (generellBrevData.erForeldreloes()) {
            barnepensjonInnvilgelse(bruker, generellBrevData, innholdMedVedlegg)
        } else {
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

    private suspend fun barnepensjonRevurdering(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
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
            generellBrevData.revurderingsaarsak,
        )
    }

    private suspend fun barnepensjonInnvilgelse(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val utbetalingsinfo = async { fetcher.hentUtbetaling() }
        val trygdetid = async { fetcher.hentTrygdetid() }
        val grunnbeloep = async { fetcher.hentGrunnbeloep() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }
        val brevutfall = async { fetcher.hentBrevutfall() }

        if (generellBrevData.erForeldreloes()) {
            BarnepensjonInnvilgelseForeldreloes.fra(
                innholdMedVedlegg,
                utbetalingsinfo.await(),
                etterbetaling.await(),
                requireNotNull(trygdetid.await()),
                requireNotNull(grunnbeloep.await()),
                generellBrevData.utlandstilknytning?.type,
                requireNotNull(brevutfall.await()),
                generellBrevData.loependeIPesys(),
                generellBrevData.personerISak.avdoede,
            )
        } else {
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

    private fun barnepensjonAvslag(
        innholdMedVedlegg: InnholdMedVedlegg,
        generellBrevData: GenerellBrevData,
    ) = BarnepensjonAvslag.fra(
        innhold = innholdMedVedlegg,
        // TODO må kunne sette brevutfall ved avslag.
        //  Det er pr nå ikke mulig da dette ligger i beregningssteget.
        brukerUnder18Aar = generellBrevData.personerISak.soeker.under18 ?: true,
        utlandstilknytning = generellBrevData.utlandstilknytning?.type,
    )

    private suspend fun barnepensjonOpphoer(
        brukerTokenInfo: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val brevutfall = async { fetcher.hentBrevutfall() }

        BarnepensjonOpphoer.fra(
            innholdMedVedlegg,
            generellBrevData.utlandstilknytning?.type,
            requireNotNull(brevutfall.await()),
        )
    }

    private suspend fun omstillingsstoenadInnvilgelse(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
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

    private suspend fun omstillingsstoenadRevurdering(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val utbetalingsinfo = async { fetcher.hentUtbetaling() }
        val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
        val forrigeAvkortingsinfo = async { fetcher.hentForrigeAvkortinginfo() }
        val trygdetid = async { fetcher.hentTrygdetid() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }
        val brevutfall = async { fetcher.hentBrevutfall() }

        OmstillingsstoenadRevurdering.fra(
            innholdMedVedlegg,
            requireNotNull(avkortingsinfo.await()),
            utbetalingsinfo.await(),
            forrigeAvkortingsinfo.await(),
            etterbetaling.await(),
            requireNotNull(trygdetid.await()),
            requireNotNull(brevutfall.await()),
            generellBrevData.revurderingsaarsak,
        )
    }

    private suspend fun omstillingsstoenadOpphoer(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val brevutfall = async { fetcher.hentBrevutfall() }

        OmstillingsstoenadOpphoer.fra(
            innholdMedVedlegg,
            generellBrevData.utlandstilknytning,
            requireNotNull(brevutfall.await()),
        )
    }
}
