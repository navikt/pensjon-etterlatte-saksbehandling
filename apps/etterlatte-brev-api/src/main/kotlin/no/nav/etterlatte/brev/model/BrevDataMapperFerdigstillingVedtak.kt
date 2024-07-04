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
import no.nav.etterlatte.brev.behandling.GenerellBrevData
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
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class BrevDataFerdigstillingRequest(
    val generellBrevData: GenerellBrevData,
    val bruker: BrukerTokenInfo,
    val innholdMedVedlegg: InnholdMedVedlegg,
    val kode: Brevkoder,
    val tittel: String? = null,
)

class BrevDataMapperFerdigstillingVedtak(
    private val beregningService: BeregningService,
    private val behandlingService: BehandlingService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val trygdetidService: TrygdetidService,
) {
    suspend fun brevDataFerdigstilling(request: BrevDataFerdigstillingRequest): BrevDataFerdigstilling {
        with(request) {
            if (generellBrevData.loependeIPesys()) {
                return fraPesys(
                    bruker,
                    innholdMedVedlegg,
                    generellBrevData.behandlingId!!,
                    generellBrevData.forenkletVedtak?.virkningstidspunkt!!,
                    generellBrevData.sak.sakType,
                    generellBrevData.erForeldreloes(),
                    generellBrevData.systemkilde,
                    generellBrevData.utlandstilknytning?.type,
                    generellBrevData.loependeIPesys(),
                    generellBrevData.personerISak.avdoede,
                    generellBrevData.personerISak.soeker.under18,
                )
            }
            return when (kode.ferdigstilling) {
                BARNEPENSJON_REVURDERING ->
                    barnepensjonRevurdering(
                        bruker,
                        innholdMedVedlegg,
                        generellBrevData.behandlingId!!,
                        generellBrevData.forenkletVedtak?.virkningstidspunkt!!,
                        generellBrevData.sak.sakType,
                        generellBrevData.utlandstilknytning?.type,
                        generellBrevData.revurderingsaarsak,
                        generellBrevData.sak.id,
                        generellBrevData.erForeldreloes(),
                        generellBrevData.personerISak.avdoede,
                    )
                BARNEPENSJON_INNVILGELSE,
                BARNEPENSJON_INNVILGELSE_FORELDRELOES,
                ->
                    barnepensjonInnvilgelse(
                        bruker,
                        innholdMedVedlegg,
                        generellBrevData.systemkilde,
                        generellBrevData.behandlingId!!,
                        generellBrevData.forenkletVedtak?.virkningstidspunkt!!,
                        generellBrevData.sak.sakType,
                        generellBrevData.erForeldreloes(),
                        generellBrevData.utlandstilknytning?.type,
                        generellBrevData.loependeIPesys(),
                        generellBrevData.personerISak.avdoede,
                    )
                BARNEPENSJON_AVSLAG ->
                    barnepensjonAvslag(
                        innholdMedVedlegg,
                        generellBrevData.personerISak.soeker.under18,
                        generellBrevData.utlandstilknytning?.type,
                    )
                BARNEPENSJON_OPPHOER ->
                    barnepensjonOpphoer(
                        bruker,
                        innholdMedVedlegg,
                        generellBrevData.behandlingId!!,
                        generellBrevData.utlandstilknytning?.type,
                        generellBrevData.forenkletVedtak?.virkningstidspunkt?.atDay(1),
                    )

                OMSTILLINGSSTOENAD_INNVILGELSE ->
                    omstillingsstoenadInnvilgelse(
                        bruker,
                        innholdMedVedlegg,
                        generellBrevData.behandlingId!!,
                        generellBrevData.forenkletVedtak!!.virkningstidspunkt!!,
                        generellBrevData.sak.sakType,
                        generellBrevData.forenkletVedtak.type,
                        generellBrevData.personerISak.avdoede,
                    )

                OMSTILLINGSSTOENAD_REVURDERING ->
                    omstillingsstoenadRevurdering(
                        bruker,
                        innholdMedVedlegg,
                        generellBrevData.behandlingId!!,
                        generellBrevData.forenkletVedtak!!.virkningstidspunkt!!,
                        generellBrevData.sak.sakType,
                        generellBrevData.forenkletVedtak.type,
                        generellBrevData.sak.id,
                        generellBrevData.revurderingsaarsak,
                        generellBrevData.personerISak.avdoede,
                    )

                OMSTILLINGSSTOENAD_AVSLAG ->
                    OmstillingsstoenadAvslag.fra(
                        innholdMedVedlegg.innhold(),
                        generellBrevData.utlandstilknytning?.type,
                    )
                OMSTILLINGSSTOENAD_OPPHOER ->
                    omstillingsstoenadOpphoer(
                        bruker,
                        innholdMedVedlegg,
                        generellBrevData.forenkletVedtak?.virkningstidspunkt?.atDay(1),
                        generellBrevData.utlandstilknytning,
                        generellBrevData.behandlingId!!,
                    )

                TILBAKEKREVING_FERDIG ->
                    TilbakekrevingBrevDTO.fra(
                        muligTilbakekreving = generellBrevData.forenkletVedtak?.tilbakekreving,
                        sakType = generellBrevData.sak.sakType,
                        utlandstilknytning = generellBrevData.utlandstilknytning?.type,
                        soeker = generellBrevData.personerISak.soeker,
                        innholdMedVedlegg.innhold(),
                    )

                AVVIST_KLAGE_FERDIG ->
                    AvvistKlageFerdigData.fra(
                        innholdMedVedlegg,
                        generellBrevData.forenkletVedtak?.klage,
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
        systemkilde: Vedtaksloesning,
        utlandstilknytningType: UtlandstilknytningType?,
        loependeIPesys: Boolean,
        avdoede: List<Avdoed>,
        soekerErUnder18: Boolean?,
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
                systemkilde,
                behandlingId,
                virkningstidspunkt,
                sakType,
                erForeldreloes,
                utlandstilknytningType,
                loependeIPesys,
                avdoede,
            )
        } else {
            BarnepensjonOmregnetNyttRegelverk.fra(
                innhold = innholdMedVedlegg,
                erUnder18Aar = soekerErUnder18,
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
        utlandstilknytningType: UtlandstilknytningType?,
        revurderingaarsak: Revurderingaarsak?,
        sakId: Long,
        erForeldreloes: Boolean,
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
        )
    }

    private suspend fun barnepensjonInnvilgelse(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        systemkilde: Vedtaksloesning,
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
        erForeldreloes: Boolean,
        utlandstilknytningType: UtlandstilknytningType?,
        loependeIPesys: Boolean,
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
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val grunnbeloep = async { beregningService.hentGrunnbeloep(bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }

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
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        sakType: SakType,
        vedtakType: VedtakType,
        sakId: Long,
        revurderingaarsak: Revurderingaarsak?,
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
            avdoede.single().navn,
            requireNotNull(vilkaarsvurdering.await()),
        )
    }

    private suspend fun omstillingsstoenadOpphoer(
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        virkningsdato: LocalDate?,
        utlandstilknytning: Utlandstilknytning?,
        behandlingId: UUID,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }

        OmstillingsstoenadOpphoer.fra(
            innholdMedVedlegg,
            virkningsdato,
            utlandstilknytning,
            requireNotNull(brevutfall.await()),
        )
    }
}
