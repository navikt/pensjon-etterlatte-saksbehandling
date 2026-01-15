package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.BrevDataRedigerbarRequest
import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.hentinformasjon.trygdetid.TrygdetidService
import no.nav.etterlatte.brev.model.bp.BarnepensjonAvslagRedigerbar
import no.nav.etterlatte.brev.model.bp.BarnepensjonForeldreloesRedigerbar
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelseRedigerbartUtfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonOpphoerRedigerbarUtfall
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurderingRedigerbartUtfall
import no.nav.etterlatte.brev.model.klage.AvvistKlageInnholdBrevData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAvslagRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadOpphoerRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurderingRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadVedtakInntektsjusteringRedigerbartUtfall
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.ForelderVerge
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import java.time.YearMonth
import java.util.UUID

class BrevDataMapperRedigerbartUtfallVedtak(
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val migreringBrevDataService: MigreringBrevDataService,
    private val trygdetidService: TrygdetidService,
) {
    suspend fun brevData(brevDataRedigerbarRequest: BrevDataRedigerbarRequest) =
        with(brevDataRedigerbarRequest) {
            if (loependeIPesys) {
                fraPesys(
                    brukerTokenInfo = brukerTokenInfo,
                    erForeldreloes = erForeldreloes,
                    behandlingId = behandlingId!!,
                    virkningstidspunkt = forenkletVedtak?.virkningstidspunkt,
                    sakType = sakType,
                    systemkilde = systemkilde,
                    loependeIPesys = true,
                    avdoede = avdoede,
                    utlandstilknytningType = utlandstilknytningType!!,
                    erForeldreloesUtenForeldreverge =
                        soekerOgEventuellVerge.soeker.foreldreloes ||
                            (avdoede.size > 1 && soekerOgEventuellVerge.verge !is ForelderVerge),
                    // Er litt usikker på hvorfor denne bruker en annen foreldreløs-sjekk enn resten av koden,
                    // men frister lite å endre på det nå når migreringa/gjenopprettinga fra pesys
                    // nærmer seg veldig ferdig
                    erSystembruker = forenkletVedtak?.saksbehandlerIdent == Fagsaksystem.EY.navn,
                )
            } else {
                brevData(
                    brukerTokenInfo,
                    sakType,
                    forenkletVedtak?.type,
                    behandlingId!!,
                    forenkletVedtak?.virkningstidspunkt,
                    erForeldreloes,
                    systemkilde,
                    false,
                    avdoede,
                    forenkletVedtak?.klage,
                    utlandstilknytningType,
                    revurderingsaarsak,
                )
            }
        }

    private suspend fun fraPesys(
        brukerTokenInfo: BrukerTokenInfo,
        erForeldreloes: Boolean,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth?,
        sakType: SakType,
        systemkilde: Vedtaksloesning,
        loependeIPesys: Boolean,
        avdoede: List<Avdoed>,
        utlandstilknytningType: UtlandstilknytningType,
        erForeldreloesUtenForeldreverge: Boolean,
        erSystembruker: Boolean,
    ): BrevDataRedigerbar {
        if (erForeldreloes) {
            return barnepensjonInnvilgelse(
                brukerTokenInfo,
                behandlingId,
                virkningstidspunkt,
                true,
                systemkilde,
                loependeIPesys,
                avdoede,
            )
        }
        return migreringBrevDataService.opprettMigreringBrevdata(
            brukerTokenInfo = brukerTokenInfo,
            systemkilde = systemkilde,
            virkningstidspunkt = virkningstidspunkt!!,
            behandlingId = behandlingId,
            sakType = sakType,
            loependeIPesys = loependeIPesys,
            utlandstilknytningType = utlandstilknytningType,
            erForeldreloes = erForeldreloesUtenForeldreverge,
            erSystembruker = erSystembruker,
        )
    }

    private suspend fun brevData(
        brukerTokenInfo: BrukerTokenInfo,
        sakType: SakType,
        vedtakType: VedtakType?,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth?,
        erForeldreloes: Boolean,
        systemkilde: Vedtaksloesning,
        loependeIPesys: Boolean,
        avdoede: List<Avdoed>,
        klage: Klage?,
        utlandstilknytningType: UtlandstilknytningType?,
        revurderingaarsak: Revurderingaarsak? = null,
    ): BrevDataRedigerbar =
        when (sakType) {
            SakType.BARNEPENSJON -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE -> {
                        barnepensjonInnvilgelse(
                            brukerTokenInfo,
                            behandlingId,
                            virkningstidspunkt,
                            erForeldreloes,
                            systemkilde,
                            loependeIPesys,
                            avdoede,
                        )
                    }

                    VedtakType.ENDRING -> {
                        barnepensjonEndring(
                            brukerTokenInfo,
                            behandlingId,
                            virkningstidspunkt,
                            utlandstilknytningType,
                        )
                    }

                    VedtakType.OPPHOER -> {
                        barnepensjonOpphoer(brukerTokenInfo, behandlingId)
                    }

                    VedtakType.AVSLAG -> {
                        barnepensjonAvslag(avdoede, brukerTokenInfo, behandlingId)
                    }

                    VedtakType.AVVIST_KLAGE -> {
                        AvvistKlageInnholdBrevData.fra(klage, utlandstilknytningType)
                    }

                    null,
                    -> {
                        ManueltBrevData()
                    }

                    VedtakType.INGEN_ENDRING,
                    VedtakType.TILBAKEKREVING,
                    -> {
                        throw InternfeilException("Brevdata for $vedtakType skal ikke utledes her")
                    }
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE -> {
                        omstillingsstoenadInnvilgelse(
                            brukerTokenInfo,
                            behandlingId,
                            virkningstidspunkt!!,
                            sakType,
                            avdoede,
                            vedtakType,
                            klage,
                        )
                    }

                    VedtakType.ENDRING -> {
                        if (revurderingaarsak != null && revurderingaarsak == Revurderingaarsak.AARLIG_INNTEKTSJUSTERING) {
                            aarligInntektsjusteringRedigerbart(
                                brukerTokenInfo,
                                behandlingId,
                                virkningstidspunkt!!,
                                sakType,
                                vedtakType,
                            )
                        } else {
                            omstillingsstoenadEndring(
                                brukerTokenInfo,
                                behandlingId,
                                virkningstidspunkt!!,
                                sakType,
                                vedtakType,
                                revurderingaarsak,
                            )
                        }
                    }

                    VedtakType.OPPHOER -> {
                        omstillingsstoenadOpphoer(brukerTokenInfo, behandlingId)
                    }

                    VedtakType.AVSLAG -> {
                        omstillingsstoenadAvslag(brukerTokenInfo, behandlingId, avdoede)
                    }

                    VedtakType.AVVIST_KLAGE -> {
                        AvvistKlageInnholdBrevData.fra(klage, utlandstilknytningType)
                    }

                    null,
                    -> {
                        ManueltBrevData()
                    }

                    VedtakType.INGEN_ENDRING,
                    VedtakType.TILBAKEKREVING,
                    -> {
                        throw InternfeilException("Brevdata for $vedtakType skal ikke utledes her")
                    }
                }
            }
        }

    private suspend fun omstillingsstoenadAvslag(
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
        avdoede: List<Avdoed>,
    ): BrevDataRedigerbar =
        coroutineScope {
            val behandling = behandlingService.hentBehandling(behandlingId, brukerTokenInfo)
            OmstillingsstoenadAvslagRedigerbartUtfall.fra(
                avdoede,
                behandling.erSluttbehandling,
                behandling.tidligereFamiliepleier,
            )
        }

    private suspend fun barnepensjonInnvilgelse(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth?,
        erForeldreloes: Boolean,
        systemkilde: Vedtaksloesning,
        loependeIPesys: Boolean,
        avdoede: List<Avdoed>,
    ) = coroutineScope {
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt!!,
                    bruker,
                )
            }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val behandling = async { behandlingService.hentBehandling(behandlingId, bruker) }.await()

        if (erForeldreloes) {
            BarnepensjonForeldreloesRedigerbar.fra(
                etterbetaling.await(),
                utbetalingsinfo = utbetalingsinfo.await(),
                avdoede = avdoede,
                systemkilde,
                loependeIPesys,
            )
        } else {
            BarnepensjonInnvilgelseRedigerbartUtfall.fra(
                utbetalingsinfo.await(),
                etterbetaling.await(),
                avdoede,
                systemkilde,
                erSluttbehandling = behandling.erSluttbehandling,
            )
        }
    }

    private suspend fun barnepensjonOpphoer(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }

        BarnepensjonOpphoerRedigerbarUtfall.fra(
            brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
        )
    }

    private suspend fun barnepensjonAvslag(
        avdoede: List<Avdoed>,
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
    ) = coroutineScope {
        val behandling = behandlingService.hentBehandling(behandlingId, bruker)
        BarnepensjonAvslagRedigerbar.fra(avdoede, behandling.erSluttbehandling)
    }

    private suspend fun barnepensjonEndring(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth?,
        utlandstilknytningType: UtlandstilknytningType?,
    ) = coroutineScope {
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt!!,
                    bruker,
                )
            }

        BarnepensjonRevurderingRedigerbartUtfall.fra(
            etterbetaling.await(),
            utbetalingsinfo.await(),
            brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
            utlandstilknytningType,
        )
    }

    private suspend fun omstillingsstoenadInnvilgelse(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
        avdoede: List<Avdoed>,
        vedtakType: VedtakType,
        klage: Klage?,
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
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }

        val behandling = behandlingService.hentBehandling(behandlingId, bruker)

        OmstillingsstoenadInnvilgelseRedigerbartUtfall.fra(
            avkortingsinfo = avkortingsinfo.await(),
            etterbetaling = etterbetaling.await(),
            behandling = behandling,
            erSluttbehandling = behandling.erSluttbehandling,
            avdoede = avdoede,
            trygdetid = krevIkkeNull(trygdetid.await()) { "Mangler trygdetid" }.single(),
            tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar ?: false,
            klage = klage,
        )
    }

    private suspend fun omstillingsstoenadEndring(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
        vedtakType: VedtakType,
        revurderingaarsak: Revurderingaarsak?,
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
        val behandling = behandlingService.hentBehandling(behandlingId, bruker)

        OmstillingsstoenadRevurderingRedigerbartUtfall.fra(
            krevIkkeNull(avkortingsinfo.await()) { "Avkortingsinfo mangler i brevutfall" },
            behandling,
            brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
            etterbetaling.await(),
            revurderingaarsak,
        )
    }

    private suspend fun aarligInntektsjusteringRedigerbart(
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

        OmstillingsstoenadVedtakInntektsjusteringRedigerbartUtfall.fra(
            avkortingsinfo = avkortingsinfo.await(),
            virkningstidspunkt = virkningstidspunkt,
        )
    }

    private suspend fun omstillingsstoenadOpphoer(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }

        OmstillingsstoenadOpphoerRedigerbartUtfall.fra(
            brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
        )
    }
}
