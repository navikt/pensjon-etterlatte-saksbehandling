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
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
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
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

data class BrevDataFerdigstillingRequest(
    val generellBrevData: GenerellBrevData,
    val bruker: BrukerTokenInfo,
    val innholdMedVedlegg: InnholdMedVedlegg,
    val kode: Brevkoder,
    val tittel: String? = null,
)

class BrevDataMapperFerdigstillingVedtak(
    private val brevdataFacade: BrevdataFacade,
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

                OMSTILLINGSSTOENAD_AVSLAG -> OmstillingsstoenadAvslag.fra(generellBrevData, innholdMedVedlegg.innhold())
                OMSTILLINGSSTOENAD_OPPHOER -> omstillingsstoenadOpphoer(bruker, generellBrevData, innholdMedVedlegg)

                TILBAKEKREVING_FERDIG -> TilbakekrevingBrevDTO.fra(generellBrevData, innholdMedVedlegg.innhold())

                AVVIST_KLAGE_FERDIG -> AvvistKlageFerdigData.fra(generellBrevData, innholdMedVedlegg)

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
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, bruker, generellBrevData)
        val utbetalingsinfo = async { fetcher.hentUtbetaling() }
        val trygdetid = async { fetcher.hentTrygdetid() }
        val grunnbeloep = async { fetcher.hentGrunnbeloep() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }

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
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val utbetalingsinfo = async { fetcher.hentUtbetaling() }
        val forrigeUtbetalingsinfo = async { fetcher.hentForrigeUtbetaling() }
        val trygdetid = async { fetcher.hentTrygdetid() }
        val grunnbeloep = async { fetcher.hentGrunnbeloep() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }
        val brevutfall = async { fetcher.hentBrevutfall() }

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
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val utbetalingsinfo = async { fetcher.hentUtbetaling() }
        val trygdetid = async { fetcher.hentTrygdetid() }
        val grunnbeloep = async { fetcher.hentGrunnbeloep() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }
        val brevutfall = async { fetcher.hentBrevutfall() }

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
        brukerTokenInfo: BrukerTokenInfo,
        innholdMedVedlegg: InnholdMedVedlegg,
        generellBrevData: GenerellBrevData,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val brevutfall = async { fetcher.hentBrevutfall() }

        BarnepensjonOpphoer.fra(
            innholdMedVedlegg,
            generellBrevData,
            generellBrevData.utlandstilknytning?.type,
            requireNotNull(brevutfall.await()),
        )
    }

    private suspend fun omstillingsstoenadInnvilgelse(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
        val trygdetid = async { fetcher.hentTrygdetid() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }
        val brevutfall = async { fetcher.hentBrevutfall() }
        val vilkaarsvurdering = async { fetcher.hentVilkaarsvurdering() }

        OmstillingsstoenadInnvilgelse.fra(
            innholdMedVedlegg,
            generellBrevData,
            avkortingsinfo.await(),
            etterbetaling.await(),
            requireNotNull(trygdetid.await()).single(),
            requireNotNull(brevutfall.await()),
            requireNotNull(vilkaarsvurdering.await()),
        )
    }

    private suspend fun omstillingsstoenadRevurdering(
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val avkortingsinfo = async { fetcher.hentAvkortinginfo() }
        val forrigeAvkortingsinfo = async { fetcher.hentForrigeAvkortinginfo() }
        val trygdetid = async { fetcher.hentTrygdetid() }
        val etterbetaling = async { fetcher.hentEtterbetaling() }
        val brevutfall = async { fetcher.hentBrevutfall() }
        val vilkaarsvurdering = async { fetcher.hentVilkaarsvurdering() }

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
        brukerTokenInfo: BrukerTokenInfo,
        generellBrevData: GenerellBrevData,
        innholdMedVedlegg: InnholdMedVedlegg,
    ) = coroutineScope {
        val fetcher = BrevDatafetcherVedtak(brevdataFacade, brukerTokenInfo, generellBrevData)
        val brevutfall = async { fetcher.hentBrevutfall() }

        OmstillingsstoenadOpphoer.fra(
            innholdMedVedlegg,
            generellBrevData,
            generellBrevData.utlandstilknytning,
            requireNotNull(brevutfall.await()),
        )
    }
}
