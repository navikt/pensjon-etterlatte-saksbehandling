package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.behandling.Avdoed
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
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.YearMonth
import java.util.UUID

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
                generellBrevData.forenkletVedtak?.virkningstidspunkt,
                generellBrevData.behandlingId!!,
                generellBrevData.sak.sakType,
                generellBrevData.erForeldreloes(),
                generellBrevData.systemkilde,
                generellBrevData.loependeIPesys(),
                generellBrevData.personerISak.avdoede,
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
                            generellBrevData.forenkletVedtak.virkningstidspunkt,
                            generellBrevData.behandlingId!!,
                            generellBrevData.sak.sakType,
                            generellBrevData.erForeldreloes(),
                            generellBrevData.systemkilde,
                            generellBrevData.loependeIPesys(),
                            generellBrevData.personerISak.avdoede,
                        )
                    VedtakType.ENDRING ->
                        barnepensjonEndring(
                            brukerTokenInfo,
                            generellBrevData.behandlingId!!,
                            generellBrevData.forenkletVedtak.virkningstidspunkt!!,
                            generellBrevData.sak.sakType,
                        )
                    VedtakType.OPPHOER -> barnepensjonOpphoer(brukerTokenInfo, generellBrevData.behandlingId!!)
                    VedtakType.AVSLAG -> ManueltBrevData()
                    VedtakType.AVVIST_KLAGE -> AvvistKlageInnholdBrevData.fra(generellBrevData.forenkletVedtak.klage)
                    VedtakType.TILBAKEKREVING,
                    null,
                    -> ManueltBrevData()
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak?.type) {
                    VedtakType.INNVILGELSE ->
                        omstillingsstoenadInnvilgelse(
                            brukerTokenInfo,
                            generellBrevData.behandlingId!!,
                            generellBrevData.forenkletVedtak.virkningstidspunkt!!,
                            generellBrevData.forenkletVedtak.type,
                            generellBrevData.sak.sakType,
                            generellBrevData.personerISak.avdoede,
                        )
                    VedtakType.ENDRING ->
                        omstillingsstoenadEndring(
                            brukerTokenInfo,
                            generellBrevData.behandlingId!!,
                            generellBrevData.forenkletVedtak.virkningstidspunkt!!,
                            generellBrevData.sak.sakType,
                            generellBrevData.forenkletVedtak.type,
                        )
                    VedtakType.OPPHOER -> omstillingsstoenadOpphoer(brukerTokenInfo, generellBrevData.behandlingId!!)
                    VedtakType.AVSLAG -> OmstillingsstoenadAvslagRedigerbartUtfall.fra(generellBrevData.personerISak.avdoede)
                    VedtakType.AVVIST_KLAGE -> AvvistKlageInnholdBrevData.fra(generellBrevData.forenkletVedtak.klage)
                    VedtakType.TILBAKEKREVING,
                    null,
                    -> ManueltBrevData()
                }
            }
        }

    private suspend fun barnepensjonInnvilgelse(
        bruker: BrukerTokenInfo,
        virkningstidspunkt: YearMonth?,
        behandlingId: UUID,
        sakType: SakType,
        erForeldreloes: Boolean,
        vedtaksloesning: Vedtaksloesning,
        loependeIPesys: Boolean,
        avdoede: List<Avdoed>,
    ) = coroutineScope {
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt!!,
                    bruker,
                    sakType,
                )
            }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }

        if (erForeldreloes) {
            BarnepensjonForeldreloesRedigerbar.fra(
                etterbetaling.await(),
                utbetalingsinfo = utbetalingsinfo.await(),
                vedtaksloesning,
                loependeIPesys,
            )
        } else {
            BarnepensjonInnvilgelseRedigerbartUtfall.fra(
                utbetalingsinfo.await(),
                etterbetaling.await(),
                vedtaksloesning,
                avdoede,
            )
        }
    }

    private suspend fun barnepensjonOpphoer(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }

        BarnepensjonOpphoerRedigerbarUtfall.fra(
            requireNotNull(brevutfall.await()),
        )
    }

    private suspend fun barnepensjonEndring(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
    ) = coroutineScope {
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt,
                    bruker,
                    sakType,
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
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        vedtakType: VedtakType,
        sakType: SakType,
        avdoede: List<Avdoed>,
    ) = coroutineScope {
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt,
                    bruker,
                    sakType,
                )
            }
        val avkortingsinfo =
            async {
                beregningService.finnAvkortingsinfo(
                    behandlingId,
                    sakType,
                    virkningstidspunkt,
                    vedtakType,
                    bruker,
                )
            }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }

        OmstillingsstoenadInnvilgelseRedigerbartUtfall.fra(
            utbetalingsinfo.await(),
            requireNotNull(avkortingsinfo.await()),
            etterbetaling.await(),
            avdoede,
        )
    }

    private suspend fun omstillingsstoenadEndring(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
        vedtakType: VedtakType,
    ) = coroutineScope {
        val avkortingsinfo =
            async {
                beregningService.finnAvkortingsinfo(
                    behandlingId,
                    sakType,
                    virkningstidspunkt,
                    vedtakType,
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
        behandlingId: UUID,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }
        OmstillingsstoenadOpphoerRedigerbartUtfall.fra(requireNotNull(brevutfall.await()))
    }
}
