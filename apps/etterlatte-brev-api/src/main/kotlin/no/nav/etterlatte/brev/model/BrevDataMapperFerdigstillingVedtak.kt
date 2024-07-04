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
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingBrevDTORequest
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

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
                return fraPesys(bruker, generellBrevData, innholdMedVedlegg)
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

                OMSTILLINGSSTOENAD_AVSLAG ->
                    OmstillingsstoenadAvslag.fra(
                        innholdMedVedlegg.innhold(),
                        generellBrevData.utlandstilknytning?.type,
                    )
                OMSTILLINGSSTOENAD_OPPHOER -> omstillingsstoenadOpphoer(bruker, generellBrevData, innholdMedVedlegg)

                TILBAKEKREVING_FERDIG ->
                    TilbakekrevingBrevDTO.fra(
                        TilbakekrevingBrevDTORequest(generellBrevData),
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
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val behandlingId = generellBrevData.behandlingId!!
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    generellBrevData.forenkletVedtak?.virkningstidspunkt!!,
                    bruker,
                    generellBrevData.sak.sakType,
                )
            }
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val grunnbeloep = async { beregningService.hentGrunnbeloep(bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }

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
                utlandstilknytning = generellBrevData.utlandstilknytning?.type,
                avdoede = generellBrevData.personerISak.avdoede,
            )
        }
    }

    private suspend fun barnepensjonRevurdering(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val behandlingId = generellBrevData.behandlingId!!
        val virkningstidspunkt = generellBrevData.forenkletVedtak?.virkningstidspunkt!!
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt,
                    bruker,
                    generellBrevData.sak.sakType,
                )
            }
        val forrigeUtbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfoNullable(
                    behandlingService.hentSisteIverksatteBehandling(generellBrevData.sak.id, bruker).id,
                    virkningstidspunkt,
                    bruker,
                    generellBrevData.sak.sakType,
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
            generellBrevData.utlandstilknytning?.type,
            requireNotNull(brevutfall.await()),
            generellBrevData.revurderingsaarsak,
            generellBrevData.erForeldreloes(),
            generellBrevData.personerISak.avdoede,
        )
    }

    private suspend fun barnepensjonInnvilgelse(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val behandlingId = generellBrevData.behandlingId!!
        val utbetalingsinfo =
            async {
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    generellBrevData.forenkletVedtak?.virkningstidspunkt!!,
                    bruker,
                    generellBrevData.sak.sakType,
                )
            }
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val grunnbeloep = async { beregningService.hentGrunnbeloep(bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val brevutfall = async { behandlingService.hentBrevutfall(behandlingId, bruker) }

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
                erGjenoppretting = generellBrevData.systemkilde == Vedtaksloesning.GJENOPPRETTA,
            )
        } else {
            BarnepensjonInnvilgelse.fra(
                innholdMedVedlegg,
                generellBrevData.personerISak.avdoede,
                utbetalingsinfo.await(),
                etterbetaling.await(),
                requireNotNull(trygdetid.await()),
                requireNotNull(grunnbeloep.await()),
                generellBrevData.utlandstilknytning?.type,
                requireNotNull(brevutfall.await()),
                erGjenoppretting = generellBrevData.systemkilde == Vedtaksloesning.GJENOPPRETTA,
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
        bruker: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(generellBrevData.behandlingId!!, bruker) }

        BarnepensjonOpphoer.fra(
            innholdMedVedlegg,
            generellBrevData.utlandstilknytning?.type,
            requireNotNull(brevutfall.await()),
            generellBrevData.forenkletVedtak?.virkningstidspunkt?.atDay(1),
        )
    }

    private suspend fun omstillingsstoenadInnvilgelse(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
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
        val trygdetid = async { trygdetidService.hentTrygdetid(behandlingId, bruker) }
        val etterbetaling = async { behandlingService.hentEtterbetaling(behandlingId, bruker) }
        val vilkaarsvurdering = async { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId, bruker) }

        OmstillingsstoenadInnvilgelse.fra(
            innholdMedVedlegg,
            avkortingsinfo.await(),
            etterbetaling.await(),
            requireNotNull(trygdetid.await()).single(),
            requireNotNull(vilkaarsvurdering.await()),
            generellBrevData.personerISak.avdoede,
        )
    }

    private suspend fun omstillingsstoenadRevurdering(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
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
        val forrigeAvkortingsinfo =
            async {
                val forrigeIverksatteBehandlingId = behandlingService.hentSisteIverksatteBehandling(generellBrevData.sak.id, bruker).id
                beregningService.finnAvkortingsinfoNullable(
                    forrigeIverksatteBehandlingId,
                    generellBrevData.sak.sakType,
                    virkningstidspunkt,
                    generellBrevData.forenkletVedtak.type,
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
            generellBrevData.revurderingsaarsak,
            generellBrevData.personerISak.avdoede
                .single()
                .navn,
            requireNotNull(vilkaarsvurdering.await()),
        )
    }

    private suspend fun omstillingsstoenadOpphoer(
        bruker: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val brevutfall = async { behandlingService.hentBrevutfall(generellBrevData.behandlingId!!, bruker) }

        OmstillingsstoenadOpphoer.fra(
            innholdMedVedlegg,
            generellBrevData.forenkletVedtak?.virkningstidspunkt?.atDay(1),
            generellBrevData.utlandstilknytning,
            requireNotNull(brevutfall.await()),
        )
    }
}
