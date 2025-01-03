package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevkoder.AVVIST_KLAGE
import no.nav.etterlatte.brev.Brevkoder.BP_AVSLAG
import no.nav.etterlatte.brev.Brevkoder.BP_INNVILGELSE
import no.nav.etterlatte.brev.Brevkoder.BP_INNVILGELSE_FORELDRELOES
import no.nav.etterlatte.brev.Brevkoder.BP_OPPHOER
import no.nav.etterlatte.brev.Brevkoder.BP_REVURDERING
import no.nav.etterlatte.brev.Brevkoder.OMS_AVSLAG
import no.nav.etterlatte.brev.Brevkoder.OMS_INNTEKTSJUSTERING_VEDTAK
import no.nav.etterlatte.brev.Brevkoder.OMS_INNVILGELSE
import no.nav.etterlatte.brev.Brevkoder.OMS_OPPHOER
import no.nav.etterlatte.brev.Brevkoder.OMS_REVURDERING
import no.nav.etterlatte.brev.Brevkoder.TILBAKEKREVING
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.hentinformasjon.trygdetid.TrygdetidService
import no.nav.etterlatte.brev.hentinformasjon.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.brev.model.bp.BarnepensjonAvslag
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelse
import no.nav.etterlatte.brev.model.bp.BarnepensjonInnvilgelseForeldreloes
import no.nav.etterlatte.brev.model.bp.BarnepensjonOmregnetNyttRegelverk
import no.nav.etterlatte.brev.model.bp.BarnepensjonOpphoer
import no.nav.etterlatte.brev.model.bp.BarnepensjonRevurdering
import no.nav.etterlatte.brev.model.klage.AvvistKlageFerdigData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAvslag
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInntektsjusteringVedtak
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelse
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadOpphoer
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurdering
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingBrevDTO
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class BrevDataFerdigstillingRequest(
    val loependeIPesys: Boolean,
    val behandlingId: UUID?,
    val sakType: SakType,
    val erForeldreloes: Boolean,
    val utlandstilknytningType: UtlandstilknytningType?,
    val avdoede: List<Avdoed>,
    val systemkilde: Vedtaksloesning,
    val soekerUnder18: Boolean?,
    val soekerNavn: String,
    val sakId: SakId,
    val virkningstidspunkt: YearMonth?,
    val vedtakType: VedtakType?,
    val revurderingsaarsak: Revurderingaarsak?,
    val tilbakekreving: Tilbakekreving?,
    val klage: Klage?,
    val harVerge: Boolean,
    val bruker: BrukerTokenInfo,
    val innholdMedVedlegg: InnholdMedVedlegg,
    val kode: Brevkoder,
    val tittel: String? = null,
)

class BrevDataMapperFerdigstillingVedtak(
    private val beregningService: BeregningService,
    private val trygdetidService: TrygdetidService,
    private val behandlingService: BehandlingService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
) {
    suspend fun brevDataFerdigstilling(request: BrevDataFerdigstillingRequest): BrevDataFerdigstilling {
        with(request) {
            if (loependeIPesys) {
                return fraPesys(
                    bruker,
                    innholdMedVedlegg,
                    behandlingId!!,
                    virkningstidspunkt!!,
                    sakType,
                    erForeldreloes,
                    utlandstilknytningType,
                    loependeIPesys,
                    avdoede,
                    systemkilde,
                    soekerUnder18,
                )
            }
            return when (kode) {
                BP_REVURDERING ->
                    barnepensjonRevurdering(
                        bruker,
                        innholdMedVedlegg,
                        behandlingId!!,
                        virkningstidspunkt!!,
                        sakType,
                        sakId,
                        utlandstilknytningType,
                        revurderingsaarsak,
                        erForeldreloes,
                        avdoede,
                        klage,
                    )

                BP_INNVILGELSE,
                BP_INNVILGELSE_FORELDRELOES,
                ->
                    barnepensjonInnvilgelse(
                        bruker,
                        innholdMedVedlegg,
                        behandlingId!!,
                        virkningstidspunkt!!,
                        sakType,
                        erForeldreloes,
                        utlandstilknytningType,
                        loependeIPesys,
                        avdoede,
                        systemkilde,
                    )

                BP_AVSLAG ->
                    barnepensjonAvslag(
                        innholdMedVedlegg,
                        soekerUnder18,
                        utlandstilknytningType,
                    )

                BP_OPPHOER ->
                    barnepensjonOpphoer(
                        bruker,
                        innholdMedVedlegg,
                        behandlingId!!,
                        utlandstilknytningType,
                        virkningstidspunkt?.atDay(1),
                    )

                OMS_INNVILGELSE ->
                    omstillingsstoenadInnvilgelse(
                        bruker,
                        innholdMedVedlegg,
                        behandlingId!!,
                        virkningstidspunkt!!,
                        sakType,
                        vedtakType!!,
                        avdoede,
                        utlandstilknytningType,
                    )

                OMS_REVURDERING ->
                    omstillingsstoenadRevurdering(
                        bruker,
                        innholdMedVedlegg,
                        revurderingsaarsak,
                        behandlingId!!,
                        sakType,
                        vedtakType!!,
                        virkningstidspunkt!!,
                        klage,
                        utlandstilknytningType,
                    )

                OMS_AVSLAG ->
                    omstillingsstoenadAvslag(
                        innholdMedVedlegg.innhold(),
                        utlandstilknytningType,
                    )

                OMS_INNTEKTSJUSTERING_VEDTAK ->
                    omstillingsstoenadInntektsjusteringVedtak(
                        bruker,
                        innholdMedVedlegg,
                        revurderingsaarsak,
                        avdoede,
                        behandlingId!!,
                        sakId,
                        sakType,
                        vedtakType!!,
                        virkningstidspunkt!!,
                    )

                OMS_OPPHOER ->
                    omstillingsstoenadOpphoer(
                        bruker,
                        innholdMedVedlegg,
                        behandlingId,
                        virkningstidspunkt?.atDay(1),
                        utlandstilknytningType,
                    )

                TILBAKEKREVING ->
                    TilbakekrevingBrevDTO.fra(
                        innholdMedVedlegg.innhold(),
                        tilbakekreving,
                        sakType,
                        utlandstilknytningType,
                        soekerNavn,
                    )

                AVVIST_KLAGE ->
                    AvvistKlageFerdigData.fra(
                        innholdMedVedlegg,
                        klage,
                        utlandstilknytningType,
                    )

                else -> throw IllegalStateException("Klarte ikke å finne brevdata for brevkode $kode for ferdigstilling.")
            }
        }
    }

    // TODO På tide å fjerne? Nei
    private suspend fun fraPesys(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
        erForeldreloes: Boolean,
        utlandstilknytningType: UtlandstilknytningType?,
        loependeIPesys: Boolean,
        avdoede: List<Avdoed>,
        systemkilde: Vedtaksloesning,
        soekerUnder18: Boolean?,
    ) = coroutineScope {
        val utbetalingsinfo =
            async {
                finnUtbetalingsinfo(behandlingId, virkningstidspunkt, bruker)
            }
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val grunnbeloep = async { beregningService.hentGrunnbeloep(bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }

        if (erForeldreloes) {
            barnepensjonInnvilgelse(
                bruker,
                innholdMedVedlegg,
                behandlingId,
                virkningstidspunkt,
                sakType,
                erForeldreloes,
                utlandstilknytningType,
                loependeIPesys,
                avdoede,
                systemkilde,
            )
        } else {
            BarnepensjonOmregnetNyttRegelverk.fra(
                innhold = innholdMedVedlegg,
                erUnder18Aar = soekerUnder18,
                utbetalingsinfo = utbetalingsinfo.await(),
                etterbetaling = etterbetaling.await(),
                trygdetid = requireNotNull(trygdetid.await()) { "Mangler trygdetid" },
                grunnbeloep = grunnbeloep.await(),
                utlandstilknytning = utlandstilknytningType,
                avdoede = avdoede,
            )
        }
    }

    private suspend fun barnepensjonRevurdering(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
        sakId: SakId,
        utlandstilknytningType: UtlandstilknytningType?,
        revurderingaarsak: Revurderingaarsak?,
        erForeldreloes: Boolean,
        avdoede: List<Avdoed>,
        klage: Klage?,
    ) = coroutineScope {
        val utbetalingsinfo =
            async {
                finnUtbetalingsinfo(behandlingId, virkningstidspunkt, bruker)
            }
        val forrigeUtbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfoNullable(
                    behandlingService.hentSisteIverksatteBehandling(sakId, bruker).id,
                    virkningstidspunkt,
                    bruker,
                )
            }
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val grunnbeloep = async { beregningService.hentGrunnbeloep(bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }

        val erMigrertYrkesskade = async { vilkaarsvurderingService.erMigrertYrkesskade(behandlingId, bruker) }

        val datoVedtakOmgjoering =
            klage
                ?.formkrav
                ?.formkrav
                ?.vedtaketKlagenGjelder
                ?.datoAttestert
                ?.toLocalDate()
        BarnepensjonRevurdering.fra(
            innholdMedVedlegg,
            utbetalingsinfo.await(),
            forrigeUtbetalingsinfo.await(),
            etterbetaling.await(),
            requireNotNull(trygdetid.await()) { "Mangler trygdetid" },
            requireNotNull(grunnbeloep.await()) { "Mangler grunnbeloep" },
            utlandstilknytningType,
            brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
            revurderingaarsak,
            erForeldreloes,
            avdoede,
            datoVedtakOmgjoering,
            erMigrertYrkesskade.await(),
        )
    }

    private suspend fun barnepensjonInnvilgelse(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
        erForeldreloes: Boolean,
        utlandstilknytningType: UtlandstilknytningType?,
        loependeIPesys: Boolean,
        avdoede: List<Avdoed>,
        systemkilde: Vedtaksloesning,
    ) = coroutineScope {
        val utbetalingsinfo =
            async {
                finnUtbetalingsinfo(behandlingId, virkningstidspunkt, bruker)
            }
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val grunnbeloep = async { beregningService.hentGrunnbeloep(bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }
        val erMigrertYrkesskade = async { vilkaarsvurderingService.erMigrertYrkesskade(behandlingId, bruker) }
        val behandling = behandlingService.hentBehandling(behandlingId, bruker)

        if (erForeldreloes) {
            BarnepensjonInnvilgelseForeldreloes.fra(
                innholdMedVedlegg,
                utbetalingsinfo.await(),
                etterbetaling.await(),
                requireNotNull(trygdetid.await()) { "Mangler trygdetid" },
                requireNotNull(grunnbeloep.await()) { "Mangler grunnbeloep" },
                utlandstilknytningType,
                brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
                loependeIPesys,
                avdoede,
                erGjenoppretting = systemkilde == Vedtaksloesning.GJENOPPRETTA,
                erMigrertYrkesskade = erMigrertYrkesskade.await(),
                erSluttbehandling = behandling.erSluttbehandling,
            )
        } else {
            BarnepensjonInnvilgelse.fra(
                innholdMedVedlegg,
                avdoede,
                utbetalingsinfo.await(),
                etterbetaling.await(),
                requireNotNull(trygdetid.await()) { "Mangler trygdetid" },
                requireNotNull(grunnbeloep.await()) { "Mangler grunnbeløp" },
                utlandstilknytningType,
                brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
                erGjenoppretting = systemkilde == Vedtaksloesning.GJENOPPRETTA,
                erMigrertYrkesskade = erMigrertYrkesskade.await(),
                erSluttbehandling = behandling.erSluttbehandling,
            )
        }
    }

    private suspend fun finnUtbetalingsinfo(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        bruker: BrukerTokenInfo,
    ): Utbetalingsinfo {
        val utbetalingsinfo =
            beregningService.finnUtbetalingsinfo(
                behandlingId,
                virkningstidspunkt,
                bruker,
            )
        if (!utbetalingsinfo.overstyrt &&
            utbetalingsinfo.beregningsperioder.any { it.harForeldreloessats == null }
        ) {
            throw ManglerHarForeldreloessats()
        }
        return utbetalingsinfo
    }

    private suspend fun barnepensjonAvslag(
        innholdMedVedlegg: InnholdMedVedlegg,
        soekerUnder18: Boolean?,
        utlandstilknytningType: UtlandstilknytningType?,
    ) = coroutineScope {
        BarnepensjonAvslag.fra(
            innhold = innholdMedVedlegg,
            // TODO må kunne sette brevutfall ved avslag.
            //  Det er pr nå ikke mulig da dette ligger i beregningssteget.
            brukerUnder18Aar = soekerUnder18 ?: true,
            utlandstilknytning = utlandstilknytningType,
        )
    }

    private suspend fun barnepensjonOpphoer(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        behandlingId: UUID,
        utlandstilknytningType: UtlandstilknytningType?,
        virkningsdato: LocalDate?,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }

        BarnepensjonOpphoer.fra(
            innholdMedVedlegg,
            utlandstilknytningType,
            brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
            virkningsdato,
        )
    }

    private suspend fun omstillingsstoenadInnvilgelse(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
        vedtakType: VedtakType,
        avdoede: List<Avdoed>,
        utlandstilknytningType: UtlandstilknytningType?,
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
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val vilkaarsvurdering = async { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId, bruker) }
        val behandling = behandlingService.hentBehandling(behandlingId, bruker)

        OmstillingsstoenadInnvilgelse.fra(
            innholdMedVedlegg,
            avkortingsinfo.await(),
            etterbetaling.await(),
            requireNotNull(trygdetid.await()) { "Mangler trygdetid" }.single(),
            requireNotNull(vilkaarsvurdering.await()) { "Mangler vilkårsvurdering" },
            avdoede,
            utlandstilknytningType,
            behandling,
        )
    }

    private suspend fun omstillingsstoenadAvslag(
        innhold: List<Slate.Element>,
        utlandstilknytningType: UtlandstilknytningType?,
    ) = coroutineScope {
        OmstillingsstoenadAvslag.fra(
            innhold,
            utlandstilknytningType,
        )
    }

    private suspend fun omstillingsstoenadRevurdering(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        revurderingaarsak: Revurderingaarsak?,
        behandlingId: UUID,
        sakType: SakType,
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        klage: Klage?,
        utlandstilknytningType: UtlandstilknytningType?,
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
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }
        val vilkaarsvurdering = async { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId, bruker) }
        val behandling = behandlingService.hentBehandling(behandlingId, bruker)

        val datoVedtakOmgjoering =
            klage
                ?.formkrav
                ?.formkrav
                ?.vedtaketKlagenGjelder
                ?.datoAttestert
                ?.toLocalDate()

        OmstillingsstoenadRevurdering.fra(
            innholdMedVedlegg,
            avkortingsinfo.await(),
            requireNotNull(trygdetid.await()) { "Mangler trygdetid" }.single(),
            brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
            revurderingaarsak,
            requireNotNull(vilkaarsvurdering.await()) { "Mangler vilkarsvurdering" },
            datoVedtakOmgjoering,
            utlandstilknytningType,
            behandling,
        )
    }

    private suspend fun omstillingsstoenadOpphoer(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        behandlingId: UUID?,
        virkningsdato: LocalDate?,
        utlandstilknytningType: UtlandstilknytningType?,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId!!, bruker) }

        OmstillingsstoenadOpphoer.fra(
            innholdMedVedlegg,
            brevutfall.await() ?: throw ManglerBrevutfall(behandlingId),
            virkningsdato,
            utlandstilknytningType,
        )
    }

    private suspend fun omstillingsstoenadInntektsjusteringVedtak(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        revurderingaarsak: Revurderingaarsak?,
        avdoede: List<Avdoed>,
        behandlingId: UUID,
        sakId: SakId,
        sakType: SakType,
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
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

        val behandling = behandlingService.hentBehandling(behandlingId, bruker)
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val vilkaarsvurdering = async { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId, bruker) }

        OmstillingsstoenadInntektsjusteringVedtak.fra(
            innholdMedVedlegg = innholdMedVedlegg,
            avkortingsinfo = avkortingsinfo.await(),
            trygdetid = requireNotNull(trygdetid.await()) { "Mangler trygdetid" }.single(),
            vilkaarsVurdering = vilkaarsvurdering.await(),
            behandling = behandling,
            navnAvdoed = avdoede.single().navn,
        )
    }
}
