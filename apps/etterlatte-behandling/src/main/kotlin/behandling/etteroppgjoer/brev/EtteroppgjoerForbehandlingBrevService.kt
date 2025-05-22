package no.nav.etterlatte.behandling.etteroppgjoer.brev

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerBrevRequestData
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.DetaljertForbehandlingDto
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.Brevkoder
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
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.util.UUID

class EtteroppgjoerForbehandlingBrevService(
    private val brevKlient: BrevKlient,
    private val grunnlagService: GrunnlagService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val behandlingService: BehandlingService,
) {
    suspend fun opprettVarselBrev(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        val brevRequest = utledBrevRequest(forbehandlingId, brukerTokenInfo)

        return brevKlient
            .opprettStrukturertBrev(
                forbehandlingId,
                brevRequest,
                brukerTokenInfo,
            ).also {
                etteroppgjoerForbehandlingService.lagreBrevreferanse(forbehandlingId, it)
            }
    }

    suspend fun tilbakestillVarselBrev(
        brevId: BrevID,
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload {
        val brevRequest = utledBrevRequest(forbehandlingId, brukerTokenInfo)

        return brevKlient.tilbakestillStrukturertBrev(
            brevID = brevId,
            behandlingId = forbehandlingId,
            brevRequest = brevRequest,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    suspend fun ferdigstillJournalfoerOgDistribuerBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        brevKlient.ferdigstillJournalfoerStrukturertBrev(
            behandlingId,
            Brevkoder.OMS_EO_FORHAANDSVARSEL.brevtype,
            brukerTokenInfo,
        )
    }

    suspend fun genererPdf(
        brevID: BrevID,
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf {
        val brevRequest = utledBrevRequest(forbehandlingId, brukerTokenInfo)

        return brevKlient.genererPdf(brevID, forbehandlingId, brevRequest, brukerTokenInfo)
    }

    suspend fun hentVarselBrev(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev? {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(forbehandlingId)
        return forbehandling.brevId?.let {
            brevKlient.hentBrev(
                sakId = forbehandling.sak.id,
                brevId = it,
                brukerTokenInfo = brukerTokenInfo,
            )
        }
    }

    private suspend fun utledBrevRequest(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevRequest =
        coroutineScope {
            val detaljertForbehandling =
                etteroppgjoerForbehandlingService.hentDetaljertForbehandling(
                    forbehandlingId,
                    brukerTokenInfo,
                )

            val pensjonsgivendeInntekt = etteroppgjoerForbehandlingService.hentPensjonsgivendeInntekt(forbehandlingId)

            val sisteIverksatteBehandling =
                behandlingService.hentBehandling(detaljertForbehandling.behandling.sisteIverksatteBehandlingId)
                    ?: throw InternfeilException("Fant ikke siste iverksatte behandling, kan ikke utlede brevinnhold")

            val (redigerbar, innhold, vedlegg, sak) =
                brevRequestDataMapper(
                    detaljertForbehandling,
                    sisteIverksatteBehandling,
                    pensjonsgivendeInntekt,
                )

            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlagForSak(sak.id)
                    ?: throw InternfeilException("Fant ikke grunnlag med sakId=${sak.id}")

            return@coroutineScope BrevRequest(
                sak = sak,
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(null),
                avdoede = grunnlag.mapAvdoede(),
                verge = hentVergeForSak(sak.sakType, null, grunnlag),
                spraak = grunnlag.mapSpraak(),
                saksbehandlerIdent = brukerTokenInfo.ident(),
                attestantIdent = null,
                skalLagre = true, // TODO: vurder riktig logikk for lagring
                brevFastInnholdData = innhold,
                brevRedigerbarInnholdData = redigerbar,
                brevVedleggData = vedlegg,
            )
        }

    private fun brevRequestDataMapper(
        data: DetaljertForbehandlingDto,
        sisteIverksatteBehandling: Behandling,
        pensjonsgivendeInntekt: PensjonsgivendeInntektFraSkatt?,
    ): EtteroppgjoerBrevRequestData {
        krevIkkeNull(data.beregnetEtteroppgjoerResultat) {
            "Beregnet etteroppgjoer resultat er null og kan ikke vises i brev"
        }

        val bosattUtland = sisteIverksatteBehandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND
        val grunnlag = data.faktiskInntekt ?: throw InternfeilException("Etteroppgjør mangler faktisk inntekt og kan ikke vises i brev")

        grunnlag.inntektInnvilgetPeriode
        // TODO: usikker om dette blir rett, følge opp ifm testing
        val norskInntekt = pensjonsgivendeInntekt != null && pensjonsgivendeInntekt.inntekter.isNotEmpty()

        return EtteroppgjoerBrevRequestData(
            redigerbar =
                EtteroppgjoerBrevData.ForhaandsvarselInnhold(
                    bosattUtland = bosattUtland,
                    norskInntekt = norskInntekt,
                    etteroppgjoersAar = data.behandling.aar,
                    rettsgebyrBeloep = Kroner(data.beregnetEtteroppgjoerResultat.grense.rettsgebyr),
                    resultatType = data.beregnetEtteroppgjoerResultat.resultatType,
                    avviksBeloep = Kroner(data.beregnetEtteroppgjoerResultat.differanse.toInt()),
                    sak = sisteIverksatteBehandling.sak,
                ),
            innhold =
                EtteroppgjoerBrevData.Forhaandsvarsel(
                    bosattUtland = bosattUtland,
                    norskInntekt = norskInntekt,
                    etteroppgjoersAar = data.behandling.aar,
                    rettsgebyrBeloep = Kroner(data.beregnetEtteroppgjoerResultat.grense.rettsgebyr),
                    resultatType = data.beregnetEtteroppgjoerResultat.resultatType,
                    stoenad = Kroner(data.beregnetEtteroppgjoerResultat.utbetaltStoenad.toInt()),
                    faktiskStoenad = Kroner(data.beregnetEtteroppgjoerResultat.nyBruttoStoenad.toInt()),
                    avviksBeloep = Kroner(data.beregnetEtteroppgjoerResultat.differanse.toInt()),
                    grunnlag = EtteroppgjoerBrevGrunnlag.fra(grunnlag),
                ),
            vedlegg =
                listOf(
                    EtteroppgjoerBrevData.beregningsVedlegg(data.behandling.aar),
                ),
            sak = sisteIverksatteBehandling.sak,
        )
    }
}
