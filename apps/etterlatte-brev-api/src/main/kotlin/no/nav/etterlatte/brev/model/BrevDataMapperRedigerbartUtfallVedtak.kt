package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.PersonerISak
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
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
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
                fraPesys(
                    brukerTokenInfo = brukerTokenInfo,
                    erForeldreloes = generellBrevData.erForeldreloes(),
                    virkningstidspunkt = generellBrevData.forenkletVedtak?.virkningstidspunkt,
                    behandlingId = generellBrevData.behandlingId!!,
                    sakType = generellBrevData.sak.sakType,
                    systemkilde = generellBrevData.systemkilde,
                    loependeIPesys = generellBrevData.loependeIPesys(),
                    avdoede = generellBrevData.personerISak.avdoede,
                    personerISak = generellBrevData.personerISak,
                    utlandstilknytning = generellBrevData.utlandstilknytning,
                    erSystembruker = generellBrevData.forenkletVedtak?.saksbehandlerIdent == Fagsaksystem.EY.navn,
                )
            } else {
                brevData(
                    brukerTokenInfo,
                    generellBrevData.sak.sakType,
                    generellBrevData.forenkletVedtak?.type,
                    generellBrevData.forenkletVedtak!!.virkningstidspunkt,
                    generellBrevData.behandlingId!!,
                    generellBrevData.erForeldreloes(),
                    generellBrevData.systemkilde,
                    generellBrevData.loependeIPesys(),
                    generellBrevData.personerISak.avdoede,
                    generellBrevData.forenkletVedtak.klage,
                )
            }
        }

    private suspend fun fraPesys(
        brukerTokenInfo: BrukerTokenInfo,
        erForeldreloes: Boolean,
        virkningstidspunkt: YearMonth?,
        behandlingId: UUID,
        sakType: SakType,
        systemkilde: Vedtaksloesning,
        loependeIPesys: Boolean,
        avdoede: List<Avdoed>,
        personerISak: PersonerISak,
        utlandstilknytning: Utlandstilknytning?,
        erSystembruker: Boolean,
    ): BrevDataRedigerbar {
        if (erForeldreloes) {
            return barnepensjonInnvilgelse(
                brukerTokenInfo,
                virkningstidspunkt,
                behandlingId,
                sakType,
                erForeldreloes,
                systemkilde,
                loependeIPesys,
                avdoede,
            )
        }
        return migreringBrevDataService.opprettMigreringBrevdata(
            brukerTokenInfo,
            systemkilde,
            virkningstidspunkt,
            behandlingId,
            sakType,
            personerISak,
            loependeIPesys,
            utlandstilknytning,
            erSystembruker,
        )
    }

    private suspend fun brevData(
        brukerTokenInfo: BrukerTokenInfo,
        sakType: SakType,
        vedtakType: VedtakType?,
        virkningstidspunkt: YearMonth?,
        behandlingId: UUID,
        erForeldreloes: Boolean,
        systemkilde: Vedtaksloesning,
        loependeIPesys: Boolean,
        avdoede: List<Avdoed>,
        klage: Klage?,
    ): BrevDataRedigerbar =
        when (sakType) {
            SakType.BARNEPENSJON -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE ->
                        barnepensjonInnvilgelse(
                            brukerTokenInfo,
                            virkningstidspunkt,
                            behandlingId,
                            sakType,
                            erForeldreloes,
                            systemkilde,
                            loependeIPesys,
                            avdoede,
                        )
                    VedtakType.ENDRING ->
                        barnepensjonEndring(
                            brukerTokenInfo,
                            behandlingId,
                            virkningstidspunkt!!,
                            sakType,
                        )
                    VedtakType.OPPHOER -> barnepensjonOpphoer(brukerTokenInfo, behandlingId)
                    VedtakType.AVSLAG -> ManueltBrevData()
                    VedtakType.AVVIST_KLAGE -> AvvistKlageInnholdBrevData.fra(klage)
                    VedtakType.TILBAKEKREVING,
                    null,
                    -> ManueltBrevData()
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE ->
                        omstillingsstoenadInnvilgelse(
                            brukerTokenInfo,
                            behandlingId,
                            virkningstidspunkt!!,
                            vedtakType,
                            sakType,
                            avdoede,
                        )
                    VedtakType.ENDRING ->
                        omstillingsstoenadEndring(
                            brukerTokenInfo,
                            behandlingId,
                            virkningstidspunkt!!,
                            sakType,
                            vedtakType,
                        )
                    VedtakType.OPPHOER -> omstillingsstoenadOpphoer(brukerTokenInfo, behandlingId)
                    VedtakType.AVSLAG -> OmstillingsstoenadAvslagRedigerbartUtfall.fra(avdoede)
                    VedtakType.AVVIST_KLAGE -> AvvistKlageInnholdBrevData.fra(klage)
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
