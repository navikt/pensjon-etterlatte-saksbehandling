package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.model.bp.BarnepensjonForeldreloesRedigerbar
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelseRedigerbartUtfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonOpphoerRedigerbarUtfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurderingRedigerbartUtfall
import no.nav.etterlatte.brev.model.klage.AvvistKlageInnholdBrevData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAvslagRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadOpphoerRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurderingRedigerbartUtfall
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.YearMonth

class BrevDataMapperRedigerbartUtfallVedtak(
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val migreringBrevDataService: MigreringBrevDataService,
) {
    suspend fun brevData(redigerbarTekstRequest: RedigerbarTekstRequest) =
        with(redigerbarTekstRequest) {
            if (generellBrevData.loependeIPesys()) {
                fraPesys(generellBrevData, brukerTokenInfo, generellBrevData.erForeldreloes())
            } else {
                brevData(generellBrevData, brukerTokenInfo)
            }
        }

    private suspend fun fraPesys(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
        erForeldreloes: Boolean,
    ): BrevDataRedigerbar {
        if (erForeldreloes) {
            return barnepensjonInnvilgelse(
                brukerTokenInfo,
                generellBrevData,
                generellBrevData.forenkletVedtak?.virkningstidspunkt,
            )
        }
        return migreringBrevDataService.opprettMigreringBrevdata(generellBrevData, brukerTokenInfo)
    }

    private suspend fun brevData(
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevDataRedigerbar =
        when (generellBrevData.sak.sakType) {
            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE ->
                        barnepensjonInnvilgelse(
                            brukerTokenInfo,
                            generellBrevData,
                            generellBrevData.forenkletVedtak?.virkningstidspunkt,
                        )
                    VedtakType.ENDRING -> barnepensjonEndring(brukerTokenInfo, generellBrevData)
                    VedtakType.OPPHOER -> barnepensjonOpphoer(brukerTokenInfo, generellBrevData)
                    VedtakType.AVSLAG -> ManueltBrevData()
                    VedtakType.AVVIST_KLAGE -> AvvistKlageInnholdBrevData.fra(generellBrevData.forenkletVedtak?.klage)
                    VedtakType.TILBAKEKREVING,
                    null,
                    -> ManueltBrevData()
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE -> omstillingsstoenadInnvilgelse(brukerTokenInfo, generellBrevData)
                    VedtakType.ENDRING -> omstillingsstoenadEndring(brukerTokenInfo, generellBrevData)
                    VedtakType.OPPHOER -> omstillingsstoenadOpphoer(brukerTokenInfo, generellBrevData)
                    VedtakType.AVSLAG -> OmstillingsstoenadAvslagRedigerbartUtfall.fra(generellBrevData.personerISak.avdoede)
                    VedtakType.AVVIST_KLAGE -> AvvistKlageInnholdBrevData.fra(generellBrevData.forenkletVedtak?.klage)
                    VedtakType.TILBAKEKREVING,
                    null,
                    -> ManueltBrevData()
                }
            }
        }

    private suspend fun barnepensjonInnvilgelse(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        virkningstidspunkt: YearMonth?,
    ) = coroutineScope {
        val behandlingId = generellBrevData.behandlingId!!
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt!!,
                    bruker,
                    generellBrevData.sak.sakType,
                )
            }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }

        if (generellBrevData.erForeldreloes()) {
            BarnepensjonForeldreloesRedigerbar.fra(
                etterbetaling.await(),
                utbetalingsinfo = utbetalingsinfo.await(),
                generellBrevData.systemkilde,
                generellBrevData.loependeIPesys(),
            )
        } else {
            BarnepensjonInnvilgelseRedigerbartUtfall.fra(
                utbetalingsinfo.await(),
                etterbetaling.await(),
                generellBrevData.systemkilde,
                generellBrevData.personerISak.avdoede,
            )
        }
    }

    private suspend fun barnepensjonOpphoer(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(generellBrevData.behandlingId!!, bruker) }

        BarnepensjonOpphoerRedigerbarUtfall.fra(
            requireNotNull(brevutfall.await()),
        )
    }

    private suspend fun barnepensjonEndring(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val behandlingId = generellBrevData.behandlingId!!
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    generellBrevData.forenkletVedtak!!.virkningstidspunkt!!,
                    bruker,
                    generellBrevData.sak.sakType,
                )
            }

        BarnepensjonRevurderingRedigerbartUtfall.fra(
            etterbetaling.await(),
            utbetalingsinfo.await(),
            requireNotNull(brevutfall.await()),
        )
    }

    private suspend fun omstillingsstoenadInnvilgelse(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val behandlingId = generellBrevData.behandlingId!!
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    generellBrevData.forenkletVedtak!!.virkningstidspunkt!!,
                    bruker,
                    generellBrevData.sak.sakType,
                )
            }
        val virkningstidspunkt = generellBrevData.forenkletVedtak!!.virkningstidspunkt!!
        val avkortingsinfo =
            async {
                beregningService.finnAvkortingsinfo(
                    behandlingId,
                    generellBrevData.sak.sakType,
                    virkningstidspunkt,
                    generellBrevData.forenkletVedtak.type,
                    bruker,
                )
            }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }

        OmstillingsstoenadInnvilgelseRedigerbartUtfall.fra(
            utbetalingsinfo.await(),
            requireNotNull(avkortingsinfo.await()),
            etterbetaling.await(),
            generellBrevData.personerISak.avdoede,
        )
    }

    private suspend fun omstillingsstoenadEndring(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val behandlingId = generellBrevData.behandlingId!!
        val virkningstidspunkt = generellBrevData.forenkletVedtak!!.virkningstidspunkt!!
        val avkortingsinfo =
            async {
                beregningService.finnAvkortingsinfo(
                    behandlingId,
                    generellBrevData.sak.sakType,
                    virkningstidspunkt,
                    generellBrevData.forenkletVedtak.type,
                    bruker,
                )
            }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }

        OmstillingsstoenadRevurderingRedigerbartUtfall.fra(
            requireNotNull(avkortingsinfo.await()),
            etterbetaling.await(),
            requireNotNull(brevutfall.await()),
        )
    }

    private suspend fun omstillingsstoenadOpphoer(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(generellBrevData.behandlingId!!, bruker) }

        OmstillingsstoenadOpphoerRedigerbartUtfall.fra(
            requireNotNull(brevutfall.await()),
        )
    }
}
