package no.nav.etterlatte.behandling.etteroppgjoer.brev

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerBrevDataMapper
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerBrevRequestData
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
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
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class EtteroppgjoerForbehandlingBrevService(
    private val brevKlient: BrevKlient,
    private val grunnlagService: GrunnlagService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val behandlingService: BehandlingService,
) {
    suspend fun opprettEtteroppgjoerBrev(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        val (redigerbartInnhold, brevInnhold, sak) = hentBrevRequestData(forbehandlingId, brukerTokenInfo)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = sak,
                    brevInnholdData = brevInnhold,
                    brevRedigerbarInnholdData = redigerbartInnhold,
                    skalLagres = false,
                    brukerTokenInfo = brukerTokenInfo,
                )
            }

        return brevKlient
            .opprettStrukturertBrev(
                forbehandlingId,
                brevRequest,
                brukerTokenInfo,
            ).also {
                etteroppgjoerForbehandlingService.lagreBrevreferanse(forbehandlingId, it)
            }
    }

    suspend fun tilbakestillEtteroppgjoerBrev(
        brevId: BrevID,
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload {
        val (redigerbartInnhold, brevInnhold, sak) = hentBrevRequestData(forbehandlingId, brukerTokenInfo)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = sak,
                    brevInnholdData = brevInnhold,
                    brevRedigerbarInnholdData = redigerbartInnhold,
                    skalLagres = false,
                    brukerTokenInfo = brukerTokenInfo,
                )
            }
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
        brevKlient.ferdigstillJournalfoerStrukturertBrev(behandlingId, Brevkoder.OMS_EO_FORHAANDSVARSEL.brevtype, brukerTokenInfo)
    }

    suspend fun genererPdf(
        brevID: BrevID,
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        skalLagres: Boolean,
    ): Pdf {
        val (redigerbartInnhold, brevInnhold, sak) = hentBrevRequestData(forbehandlingId, brukerTokenInfo)
        val request =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = sak,
                    brevInnholdData = brevInnhold,
                    brevRedigerbarInnholdData = redigerbartInnhold,
                    skalLagres = skalLagres, // TODO: utlede dette for etteroppgjørbrev
                    brukerTokenInfo = brukerTokenInfo,
                )
            }

        return brevKlient.genererPdf(brevID, forbehandlingId, request, brukerTokenInfo)
    }

    suspend fun hentEtteroppgjoersbrev(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev? {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(forbehandlingId)
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
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBrevRequestData {
        val detaljertForbehandling =
            etteroppgjoerForbehandlingService.hentDetaljertForbehandling(
                forbehandlingId,
                brukerTokenInfo,
            )

        val sisteIverksatteBehandling = behandlingService.hentSisteIverksatte(detaljertForbehandling.behandling.sak.id)
        krevIkkeNull(sisteIverksatteBehandling) {
            "Fant ikke siste iverksatte behandling og kan ikke utlede brevdata"
        }

        val pensjonsgivendeInntekt = etteroppgjoerForbehandlingService.hentPensjonsgivendeInntekt(forbehandlingId)

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
