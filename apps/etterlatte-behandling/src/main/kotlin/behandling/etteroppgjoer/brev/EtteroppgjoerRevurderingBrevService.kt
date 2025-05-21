package no.nav.etterlatte.behandling.etteroppgjoer.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.Pdf
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.hentVergeForSak
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevData
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevGrunnlag
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.util.UUID

class EtteroppgjoerRevurderingBrevService(
    private val grunnlagService: GrunnlagService,
    private val vedtakKlient: VedtakKlient,
    private val brevKlient: BrevKlient,
    private val behandlingService: BehandlingService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val beregningKlient: BeregningKlient,
    private val brevApiKlient: BrevApiKlient,
) {
    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Brev {
        val brevRequest = retryOgPakkUt { utledBrevRequest(bruker, behandlingId, sakId) }

        return brevKlient.opprettStrukturertBrev(
            behandlingId,
            brevRequest,
            bruker,
        )
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
        skalLagres: Boolean,
    ): Pdf {
        val brevRequest = retryOgPakkUt { utledBrevRequest(bruker, behandlingId, sakId, skalLagres) }

        return brevKlient.genererPdf(brevID, behandlingId, brevRequest, bruker)
    }

    suspend fun tilbakestillVedtaksbrev(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): BrevPayload {
        val brevRequest = retryOgPakkUt { utledBrevRequest(bruker, behandlingId, sakId) }

        return brevKlient.tilbakestillStrukturertBrev(
            brevID,
            behandlingId,
            brevRequest,
            bruker,
        )
    }

    suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        brevKlient.ferdigstillStrukturertBrev(behandlingId, Brevtype.VEDTAK, brukerTokenInfo)
    }

    suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev? = brevKlient.hentVedtaksbrev(behandlingId, bruker)

    private suspend fun utledBrevRequest(
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
        skalLagres: Boolean = false,
    ): BrevRequest =
        coroutineScope {
            val vedtakDeferred = async { vedtakKlient.hentVedtak(behandlingId, brukerTokenInfo) }

            val vedtak =
                vedtakDeferred.await()
                    ?: throw InternfeilException("Fant ikke vedtak for behandlingId=$behandlingId")

            val behandling =
                behandlingService.hentBehandling(behandlingId) ?: throw InternfeilException("Fant ikke behandlingId=$behandlingId")

            val avkorting = beregningKlient.hentBeregningOgAvkorting(behandlingId, brukerTokenInfo)
            val sisteUtbetaltBeloep = avkorting.perioder.maxBy { it.periode.fom }.ytelseEtterAvkorting

            val detaljertForbehandling =
                etteroppgjoerForbehandlingService.hentDetaljertForbehandling(
                    UUID.fromString(behandling.relatertBehandlingId),
                    brukerTokenInfo,
                )

            val sisteIverksatteBehandling =
                behandlingService.hentBehandling(detaljertForbehandling.behandling.sisteIverksatteBehandlingId)
                    ?: throw InternfeilException("Fant ikke siste iverksatte behandling, kan ikke utlede brevinnhold")

            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlagForSak(detaljertForbehandling.behandling.sak.id)
                    ?: throw InternfeilException("Fant ikke grunnlag med sakId=$sakId")

            val beregnetEtteroppgjoerResultat =
                detaljertForbehandling.beregnetEtteroppgjoerResultat
                    ?: throw InternfeilException("Fant ikke etteroppgjoerResultat for behandlingId=$behandlingId")

            val faktiskInntekt =
                detaljertForbehandling.faktiskInntekt
                    ?: throw InternfeilException("Etteroppgjør mangler faktisk inntekt og kan ikke vises i brev")

            val sak = detaljertForbehandling.behandling.sak

            val forhaandsvarselBrev =
                brevApiKlient
                    .hentBrev(sak.id, detaljertForbehandling.behandling.brevId!!, brukerTokenInfo)

            krev(forhaandsvarselBrev.erDistribuert()) {
                "Finner ingen distribuerte forhåndsvarsel om etteroppgjør"
            }

            BrevRequest(
                sak = sak,
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(null),
                avdoede = grunnlag.mapAvdoede(),
                verge = hentVergeForSak(sak.sakType, null, grunnlag),
                spraak = grunnlag.mapSpraak(),
                saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: brukerTokenInfo.ident(),
                attestantIdent = vedtak.attestasjon?.attestant ?: brukerTokenInfo.ident(),
                skalLagre = skalLagres,
                brevFastInnholdData =
                    EtteroppgjoerBrevData.Vedtak(
                        bosattUtland = sisteIverksatteBehandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                        etteroppgjoersAar = detaljertForbehandling.behandling.aar,
                        avviksBeloep = Kroner(beregnetEtteroppgjoerResultat.differanse.toInt()),
                        utbetaltBeloep = Kroner(sisteUtbetaltBeloep),
                        resultatType = beregnetEtteroppgjoerResultat.resultatType,
                        inntekt = Kroner(beregnetEtteroppgjoerResultat.utbetaltStoenad.toInt()),
                        faktiskInntekt = Kroner(beregnetEtteroppgjoerResultat.nyBruttoStoenad.toInt()),
                        grunnlag = EtteroppgjoerBrevGrunnlag.fra(faktiskInntekt),
                    ),
                brevRedigerbarInnholdData =
                    EtteroppgjoerBrevData.VedtakInnhold(
                        etteroppgjoersAar = detaljertForbehandling.behandling.aar,
                        forhaandsvarselSendtDato = forhaandsvarselBrev.statusEndret.toLocalDate(),
                        mottattSvarDato = null, // TODO: legg til dato for mottatt journalpost
                    ),
                brevVedleggData =
                    listOf(
                        EtteroppgjoerBrevData.beregningsVedlegg(detaljertForbehandling.behandling.aar),
                    ),
            )
        }
}
