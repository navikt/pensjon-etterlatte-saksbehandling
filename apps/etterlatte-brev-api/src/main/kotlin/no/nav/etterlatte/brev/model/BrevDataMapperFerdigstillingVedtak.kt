package no.nav.etterlatte.brev.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.EtterlatteBrevKode.AVVIST_KLAGE_FERDIG
import no.nav.etterlatte.brev.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_FORELDRELOES
import no.nav.etterlatte.brev.EtterlatteBrevKode.BARNEPENSJON_OPPHOER
import no.nav.etterlatte.brev.EtterlatteBrevKode.BARNEPENSJON_REVURDERING
import no.nav.etterlatte.brev.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG
import no.nav.etterlatte.brev.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE
import no.nav.etterlatte.brev.EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER
import no.nav.etterlatte.brev.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING
import no.nav.etterlatte.brev.EtterlatteBrevKode.TILBAKEKREVING_FERDIG
import no.nav.etterlatte.brev.behandling.Avdoed
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
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelse
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadOpphoer
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurdering
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingBrevDTO
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
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
    val sakId: Long,
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
            return when (kode.ferdigstilling) {
                BARNEPENSJON_REVURDERING ->
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
                BARNEPENSJON_INNVILGELSE,
                BARNEPENSJON_INNVILGELSE_FORELDRELOES,
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
                BARNEPENSJON_AVSLAG ->
                    barnepensjonAvslag(
                        innholdMedVedlegg,
                        soekerUnder18,
                        utlandstilknytningType,
                    )
                BARNEPENSJON_OPPHOER ->
                    barnepensjonOpphoer(
                        bruker,
                        innholdMedVedlegg,
                        behandlingId!!,
                        utlandstilknytningType,
                        virkningstidspunkt?.atDay(1),
                    )

                OMSTILLINGSSTOENAD_INNVILGELSE ->
                    omstillingsstoenadInnvilgelse(
                        bruker,
                        innholdMedVedlegg,
                        behandlingId!!,
                        virkningstidspunkt!!,
                        sakType,
                        vedtakType!!,
                        avdoede,
                    )

                OMSTILLINGSSTOENAD_REVURDERING ->
                    omstillingsstoenadRevurdering(
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

                OMSTILLINGSSTOENAD_AVSLAG ->
                    OmstillingsstoenadAvslag.fra(
                        innholdMedVedlegg.innhold(),
                        utlandstilknytningType,
                    )
                OMSTILLINGSSTOENAD_OPPHOER ->
                    omstillingsstoenadOpphoer(
                        bruker,
                        innholdMedVedlegg,
                        behandlingId,
                        virkningstidspunkt?.atDay(1),
                        utlandstilknytningType,
                    )

                TILBAKEKREVING_FERDIG ->
                    TilbakekrevingBrevDTO.fra(
                        innholdMedVedlegg.innhold(),
                        tilbakekreving,
                        sakType,
                        utlandstilknytningType,
                        soekerNavn,
                    )

                AVVIST_KLAGE_FERDIG ->
                    AvvistKlageFerdigData.fra(
                        innholdMedVedlegg,
                        klage,
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
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt,
                    bruker,
                    sakType,
                )
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
                trygdetid = requireNotNull(trygdetid.await()),
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
        sakId: Long,
        utlandstilknytningType: UtlandstilknytningType?,
        revurderingaarsak: Revurderingaarsak?,
        erForeldreloes: Boolean,
        avdoede: List<Avdoed>,
        klage: Klage?,
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
        val forrigeUtbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfoNullable(
                    behandlingService.hentSisteIverksatteBehandling(sakId, bruker).id,
                    virkningstidspunkt,
                    bruker,
                    sakType,
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
            requireNotNull(trygdetid.await()),
            requireNotNull(grunnbeloep.await()),
            utlandstilknytningType,
            requireNotNull(brevutfall.await()),
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
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt,
                    bruker,
                    sakType,
                )
            }
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val grunnbeloep = async { beregningService.hentGrunnbeloep(bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }
        val erMigrertYrkesskade = async { vilkaarsvurderingService.erMigrertYrkesskade(behandlingId, bruker) }

        if (erForeldreloes) {
            BarnepensjonInnvilgelseForeldreloes.fra(
                innholdMedVedlegg,
                utbetalingsinfo.await(),
                etterbetaling.await(),
                requireNotNull(trygdetid.await()),
                requireNotNull(grunnbeloep.await()),
                utlandstilknytningType,
                requireNotNull(brevutfall.await()),
                loependeIPesys,
                avdoede,
                erGjenoppretting = systemkilde == Vedtaksloesning.GJENOPPRETTA,
                erMigrertYrkesskade = erMigrertYrkesskade.await(),
            )
        } else {
            BarnepensjonInnvilgelse.fra(
                innholdMedVedlegg,
                avdoede,
                utbetalingsinfo.await(),
                etterbetaling.await(),
                requireNotNull(trygdetid.await()),
                requireNotNull(grunnbeloep.await()),
                utlandstilknytningType,
                requireNotNull(brevutfall.await()),
                erGjenoppretting = systemkilde == Vedtaksloesning.GJENOPPRETTA,
                erMigrertYrkesskade = erMigrertYrkesskade.await(),
            )
        }
    }

    private fun barnepensjonAvslag(
        innholdMedVedlegg: InnholdMedVedlegg,
        soekerUnder18: Boolean?,
        utlandstilknytningType: UtlandstilknytningType?,
    ) = BarnepensjonAvslag.fra(
        innhold = innholdMedVedlegg,
        // TODO må kunne sette brevutfall ved avslag.
        //  Det er pr nå ikke mulig da dette ligger i beregningssteget.
        brukerUnder18Aar = soekerUnder18 ?: true,
        utlandstilknytning = utlandstilknytningType,
    )

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
            requireNotNull(brevutfall.await()),
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

        OmstillingsstoenadInnvilgelse.fra(
            innholdMedVedlegg,
            avkortingsinfo.await(),
            etterbetaling.await(),
            requireNotNull(trygdetid.await()).single(),
            requireNotNull(vilkaarsvurdering.await()),
            avdoede,
        )
    }

    private suspend fun omstillingsstoenadRevurdering(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        revurderingaarsak: Revurderingaarsak?,
        avdoede: List<Avdoed>,
        behandlingId: UUID,
        sakId: Long,
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
        val forrigeAvkortingsinfo =
            async {
                val forrigeIverksatteBehandlingId = behandlingService.hentSisteIverksatteBehandling(sakId, bruker).id
                beregningService.finnAvkortingsinfoNullable(
                    forrigeIverksatteBehandlingId,
                    sakType,
                    virkningstidspunkt,
                    vedtakType,
                    bruker,
                )
            }
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }
        val vilkaarsvurdering = async { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId, bruker) }

        OmstillingsstoenadRevurdering.fra(
            innholdMedVedlegg,
            avkortingsinfo.await(),
            forrigeAvkortingsinfo.await(),
            etterbetaling.await(),
            requireNotNull(trygdetid.await()).single(),
            requireNotNull(brevutfall.await()),
            revurderingaarsak,
            avdoede
                .single()
                .navn,
            requireNotNull(vilkaarsvurdering.await()),
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
            requireNotNull(brevutfall.await()),
            virkningsdato,
            utlandstilknytningType,
        )
    }
}
