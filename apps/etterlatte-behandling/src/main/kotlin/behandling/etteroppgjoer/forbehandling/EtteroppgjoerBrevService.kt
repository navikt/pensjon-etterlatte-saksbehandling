package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerBrevMapper
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
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
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(brukerTokenInfo, forbehandlingId)
        val brevData = EtteroppgjoerBrevMapper.fra(forbehandling)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.behandling.sak,
                    brevInnholdData = brevData.innhold,
                    brevRedigerbarInnholdData = brevData.redigerbar,
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
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(brukerTokenInfo, forbehandlingId)
        val (redigerbartInnhold, brevInnhold) = EtteroppgjoerBrevMapper.fra(forbehandling)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.behandling.sak,
                    brevInnholdData = brevInnhold,
                    skalLagres = false,
                    brukerTokenInfo = brukerTokenInfo,
                    brevRedigerbarInnholdData = redigerbartInnhold,
                )
            }
        return brevKlient.tilbakestillStrukturertBrev(
            brevID = brevId,
            behandlingId = forbehandlingId,
            brevRequest = brevRequest,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    private suspend fun utledBrevRequest(
        sak: Sak,
        brevInnholdData: BrevFastInnholdData,
        brevRedigerbarInnholdData: BrevDataRedigerbar,
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

    suspend fun ferdigstillBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(brukerTokenInfo, behandlingId)
        val brevData = EtteroppgjoerBrevMapper.fra(forbehandling)

        brevKlient.ferdigstillStrukturertBrev(behandlingId, brevData.innhold.brevKode.brevtype, brukerTokenInfo)
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Pdf {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(bruker, behandlingId)
        val brevData = EtteroppgjoerBrevMapper.fra(forbehandling)
        val request =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.behandling.sak,
                    brevInnholdData = brevData.innhold,
                    brevRedigerbarInnholdData = brevData.redigerbar,
                    skalLagres = false, // TODO: utlede dette for etteroppgjørbrev
                    brukerTokenInfo = bruker,
                )
            }

        return brevKlient.genererPdf(brevID, behandlingId, request, bruker)
    }

    suspend fun hentEtteroppgjoersbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev? {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(bruker, behandlingId)
        if (forbehandling.behandling.brevId == null) {
            return null
        }

        return brevApiKlient.hentBrev(
            sakId = forbehandling.behandling.sak.id,
            brevId = forbehandling.behandling.brevId,
            brukerTokenInfo = bruker,
        )
    }
}
