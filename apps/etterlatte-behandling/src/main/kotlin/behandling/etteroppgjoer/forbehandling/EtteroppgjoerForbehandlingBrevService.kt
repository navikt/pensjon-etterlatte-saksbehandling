package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerBrevDataMapper
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerBrevRequestData
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
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
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class EtteroppgjoerForbehandlingBrevService(
    private val brevKlient: BrevKlient,
    private val grunnlagService: GrunnlagService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val behandlingService: BehandlingService,
) {
    suspend fun opprettEtteroppgjoerBrev(
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        val (redigerbartInnhold, brevInnhold, forbehandling) = hentBrevRequestData(behandlingId, sakId, brukerTokenInfo)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.behandling.sak,
                    brevInnholdData = brevInnhold,
                    brevRedigerbarInnholdData = redigerbartInnhold,
                    skalLagres = false,
                    brukerTokenInfo = brukerTokenInfo,
                )
            }

        return brevKlient
            .opprettStrukturertBrev(
                behandlingId,
                brevRequest,
                brukerTokenInfo,
            ).also {
                etteroppgjoerForbehandlingService.lagreBrevreferanse(behandlingId, it)
            }
    }

    suspend fun tilbakestillEtteroppgjoerBrev(
        brevId: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload {
        val (redigerbartInnhold, brevInnhold, forbehandling) = hentBrevRequestData(behandlingId, sakId, brukerTokenInfo)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.behandling.sak,
                    brevInnholdData = brevInnhold,
                    brevRedigerbarInnholdData = redigerbartInnhold,
                    skalLagres = false,
                    brukerTokenInfo = brukerTokenInfo,
                )
            }
        return brevKlient.tilbakestillStrukturertBrev(
            brevID = brevId,
            behandlingId = behandlingId,
            brevRequest = brevRequest,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    suspend fun ferdigstillOgDistribuerBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        brevKlient.ferdigstillStrukturertBrev(behandlingId, Brevkoder.OMS_EO_FORHAANDSVARSEL.brevtype, brukerTokenInfo)

        // TODO: se klage/aktivitetsplikt

        // TODO: journalfoer brev
        // TODO: distribuer brev
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
        skalLagres: Boolean,
    ): Pdf {
        val (redigerbartInnhold, brevInnhold, forbehandling) = hentBrevRequestData(behandlingId, sakId, brukerTokenInfo)
        val request =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.behandling.sak,
                    brevInnholdData = brevInnhold,
                    brevRedigerbarInnholdData = redigerbartInnhold,
                    skalLagres = skalLagres, // TODO: utlede dette for etteroppgjørbrev
                    brukerTokenInfo = brukerTokenInfo,
                )
            }

        return brevKlient.genererPdf(brevID, behandlingId, request, brukerTokenInfo)
    }

    suspend fun hentEtteroppgjoersbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev? {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(behandlingId)
        if (forbehandling.brevId == null) {
            return null
        }

        return brevKlient.hentBrev(
            sakId = forbehandling.sak.id,
            brevId = forbehandling.brevId,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    private fun hentBrevRequestData(
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBrevRequestData {
        val detaljertForbehandling =
            etteroppgjoerForbehandlingService.hentDetaljertForbehandling(
                behandlingId,
                brukerTokenInfo,
            )

        val sisteIverksatteBehandling = behandlingService.hentSisteIverksatte(sakId)
        krevIkkeNull(sisteIverksatteBehandling) {
            "Fant ikke siste iverksatte behandling og kan ikke stadfeste bosattUtland"
        }

        val pensjonsgivendeInntekt = etteroppgjoerForbehandlingService.hentPensjonsgivendeInntekt(behandlingId)

        return EtteroppgjoerBrevDataMapper.fra(detaljertForbehandling, sisteIverksatteBehandling, pensjonsgivendeInntekt)
    }

    private suspend fun utledBrevRequest(
        sak: Sak,
        brevInnholdData: BrevFastInnholdData,
        brevRedigerbarInnholdData: BrevRedigerbarInnholdData?,
        skalLagres: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevRequest =
        coroutineScope {
            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlagForSak(sak.id)
                    ?: throw InternfeilException("Fant ikke grunnlag med sakId=${sak.id}")

            val verge = hentVergeForSak(sak.sakType, null, grunnlag)
            val soeker = grunnlag.mapSoeker(null)

            val innloggetSaksbehandlerIdent = brukerTokenInfo.ident()

            BrevRequest(
                sak = sak,
                innsender = grunnlag.mapInnsender(),
                soeker = soeker,
                avdoede = grunnlag.mapAvdoede(),
                verge = verge,
                spraak = grunnlag.mapSpraak(),
                saksbehandlerIdent = innloggetSaksbehandlerIdent,
                attestantIdent = null,
                skalLagre = skalLagres,
                brevFastInnholdData = brevInnholdData,
                brevRedigerbarInnholdData = brevRedigerbarInnholdData,
            )
        }
}
