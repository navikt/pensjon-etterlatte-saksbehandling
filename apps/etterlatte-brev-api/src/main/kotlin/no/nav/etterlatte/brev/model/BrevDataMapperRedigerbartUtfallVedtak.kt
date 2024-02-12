package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.bp.BarnepensjonForeldreloesRedigerbar
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelseRedigerbartUtfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurderingRedigerbartUtfall
import no.nav.etterlatte.brev.model.klage.AvvistKlageInnholdBrevData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAvslag
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadOpphoerRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurderingRedigerbartUtfall
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingInnholdBrevData
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.BrukerTokenInfo

class BrevDataMapperRedigerbartUtfallVedtak(
    private val brevdataFacade: BrevdataFacade,
    private val migreringBrevDataService: MigreringBrevDataService,
) {
    suspend fun brevData(redigerbarTekstRequest: RedigerbarTekstRequest) =
        with(redigerbarTekstRequest) {
            if (generellBrevData.loependeIPesys()) {
                fraPesys(generellBrevData, brukerTokenInfo, migrering)
            } else {
                brevData(generellBrevData, brukerTokenInfo)
            }
        }

    private suspend fun fraPesys(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
        migrering: MigreringBrevRequest? = null,
    ): BrevData {
        if (generellBrevData.erForeldreloes()) {
            return barnepensjonInnvilgelse(brukerTokenInfo, generellBrevData)
        }
        return migreringBrevDataService.opprettMigreringBrevdata(generellBrevData, migrering, brukerTokenInfo)
    }

    private suspend fun brevData(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevData =
        when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> barnepensjonInnvilgelse(brukerTokenInfo, generellBrevData)
                    VedtakType.ENDRING -> barnepensjonEndring(brukerTokenInfo, generellBrevData)
                    VedtakType.AVSLAG -> ManueltBrevData()
                    VedtakType.OPPHOER -> ManueltBrevData()
                    VedtakType.TILBAKEKREVING -> TilbakekrevingInnholdBrevData.fra(generellBrevData)
                    VedtakType.AVVIST_KLAGE -> AvvistKlageInnholdBrevData.fra(generellBrevData)
                    null -> ManueltBrevData()
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> omstillingsstoenadInnvilgelse(brukerTokenInfo, generellBrevData)
                    VedtakType.ENDRING -> omstillingsstoenadEndring(brukerTokenInfo, generellBrevData)
                    VedtakType.AVSLAG -> OmstillingsstoenadAvslag.fra(generellBrevData, emptyList())
                    VedtakType.OPPHOER -> OmstillingsstoenadOpphoerRedigerbartUtfall.fra(generellBrevData, emptyList())
                    VedtakType.TILBAKEKREVING -> TilbakekrevingInnholdBrevData.fra(generellBrevData)
                    VedtakType.AVVIST_KLAGE -> AvvistKlageInnholdBrevData.fra(generellBrevData)
                    null -> ManueltBrevData()
                }
            }
        }

    private suspend fun barnepensjonInnvilgelse(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
        val utbetalingsinfo = async { fetcher.hentUtbetaling() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }

        if (generellBrevData.erForeldreloes()) {
            BarnepensjonForeldreloesRedigerbar.fra(
                generellBrevData,
                etterbetaling.await(),
            )
        } else {
            BarnepensjonInnvilgelseRedigerbartUtfall.fra(
                generellBrevData,
                utbetalingsinfo.await(),
                etterbetaling.await(),
            )
        }
    }

    private suspend fun barnepensjonEndring(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val fetcher = BrevDatafetcher(brevdataFacade, brukerTokenInfo, generellBrevData)
        val etterbetaling = async { fetcher.hentEtterbetaling() }

        BarnepensjonRevurderingRedigerbartUtfall.fra(etterbetaling.await())
    }

    private suspend fun omstillingsstoenadInnvilgelse(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val fetcher = BrevDatafetcher(brevdataFacade, bruker, generellBrevData)
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

    private suspend fun omstillingsstoenadEndring(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val fetcher = BrevDatafetcher(brevdataFacade, bruker, generellBrevData)
        val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }
        val brevutfall = async { fetcher.hentBrevutfall() }

        OmstillingsstoenadRevurderingRedigerbartUtfall.fra(
            requireNotNull(avkortingsinfo.await()),
            etterbetaling.await(),
            requireNotNull(brevutfall.await()),
        )
    }
}
