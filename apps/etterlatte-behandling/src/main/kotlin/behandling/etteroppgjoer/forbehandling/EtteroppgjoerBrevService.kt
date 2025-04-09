package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerBrevDataMapper
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerBrevRequestData
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.BrevRequest
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
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class EtteroppgjoerBrevService(
    private val brevKlient: BrevKlient,
    private val brevApiKlient: BrevApiKlient,
    private val grunnlagService: GrunnlagService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
) {
    suspend fun opprettEtteroppgjoerBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        val (redigerbartInnhold, brevInnhold, forbehandling) = hentBrevRequestData(behandlingId, brukerTokenInfo)

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
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload {
        val (redigerbartInnhold, brevInnhold, forbehandling) = hentBrevRequestData(behandlingId, brukerTokenInfo)

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

    suspend fun ferdigstillBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val (redigerbartInnhold, brevInnhold, forbehandling) = hentBrevRequestData(behandlingId, brukerTokenInfo)
        brevKlient.ferdigstillStrukturertBrev(behandlingId, brevInnhold.brevKode.brevtype, brukerTokenInfo)
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf {
        val (redigerbartInnhold, brevInnhold, forbehandling) = hentBrevRequestData(behandlingId, brukerTokenInfo)
        val request =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.behandling.sak,
                    brevInnholdData = brevInnhold,
                    brevRedigerbarInnholdData = redigerbartInnhold,
                    skalLagres = false, // TODO: utlede dette for etteroppgjørbrev
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

        return brevApiKlient.hentBrev(
            sakId = forbehandling.sak.id,
            brevId = forbehandling.brevId,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    private fun hentBrevRequestData(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBrevRequestData {
        val detaljertForbehandling =
            etteroppgjoerForbehandlingService.hentDetaljertForbehandling(
                behandlingId,
                brukerTokenInfo,
            )

        return EtteroppgjoerBrevDataMapper.fra(detaljertForbehandling)
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
