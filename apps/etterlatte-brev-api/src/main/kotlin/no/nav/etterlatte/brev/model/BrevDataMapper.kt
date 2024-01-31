package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelseRedigerbartUtfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurderingRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAvslag
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadOpphoerRedigerbartUtfall
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingInnholdBrevData
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.BrukerTokenInfo

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
                            val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
                            val utbetalingsinfo = async { fetcher.hentUtbetaling() }
                            val etterbetaling = async { fetcher.hentEtterbetaling() }

                            BarnepensjonInnvilgelseRedigerbartUtfall.fra(
                                generellBrevData,
                                utbetalingsinfo.await(),
                                etterbetaling.await(),
                            )
                        }

                    VedtakType.ENDRING ->
                        coroutineScope {
                            val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
                            val etterbetaling = async { fetcher.hentEtterbetaling() }

                            BarnepensjonRevurderingRedigerbartUtfall.fra(etterbetaling.await())
                        }

                    VedtakType.AVSLAG -> ManueltBrevData()
                    VedtakType.OPPHOER -> ManueltBrevData()
                    VedtakType.TILBAKEKREVING -> TilbakekrevingInnholdBrevData.fra(generellBrevData)
                    null -> ManueltBrevData()
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> {
                        coroutineScope {
                            val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
                            val utbetalingsinfo = async { fetcher.hentUtbetaling() }
                            val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
                            val etterbetaling = async { fetcher.hentEtterbetaling() }

                            OmstillingsstoenadInnvilgelseRedigerbartUtfall.fra(
                                generellBrevData,
                                utbetalingsinfo.await(),
                                requireNotNull(avkortingsinfo.await()),
                                etterbetaling.await(),
                            )
                        }
                    }

                    VedtakType.ENDRING -> ManueltBrevData()
                    VedtakType.AVSLAG -> OmstillingsstoenadAvslag.fra(generellBrevData, emptyList())
                    VedtakType.OPPHOER -> OmstillingsstoenadOpphoerRedigerbartUtfall.fra(generellBrevData, emptyList())
                    VedtakType.TILBAKEKREVING -> TilbakekrevingInnholdBrevData.fra(generellBrevData)
                    null -> ManueltBrevData()
                }
            }
        }
    }
}
