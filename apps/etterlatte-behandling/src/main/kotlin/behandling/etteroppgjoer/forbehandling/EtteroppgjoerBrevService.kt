package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandling
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
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevData
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
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
        val forbehandling =
            etteroppgjoerForbehandlingService.hentDetaljertForbehandling(
                forbehandlingId,
                HardkodaSystembruker.etteroppgjoer,
            )
        val brevInnholdData = utledBrevInnholdData(forbehandling.behandling)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.behandling.sak,
                    brevInnholdData = brevInnholdData,
                    brevRedigerbarInnholdData = null,
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
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(forbehandlingId)
        val brevInnholdData = utledBrevInnholdData(forbehandling)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.sak,
                    brevInnholdData = brevInnholdData,
                    skalLagres = false,
                    brukerTokenInfo = brukerTokenInfo,
                    brevRedigerbarInnholdData = null,
                )
            }
        return brevKlient.tilbakestillStrukturertBrev(
            brevID = brevId,
            behandlingId = forbehandlingId,
            brevRequest = brevRequest,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    suspend fun ferdigstillBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(behandlingId)
        val brevInnholdData = utledBrevInnholdData(forbehandling)

        brevKlient.ferdigstillStrukturertBrev(behandlingId, brevInnholdData.brevKode.brevtype, brukerTokenInfo)
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Pdf {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(behandlingId)
        val brevInnholdData = utledBrevInnholdData(forbehandling)

        val request =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = forbehandling.sak,
                    brevInnholdData = brevInnholdData,
                    brevRedigerbarInnholdData = null,
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
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(behandlingId)
        if (forbehandling.brevId == null) {
            return null
        }

        return brevApiKlient.hentBrev(
            sakId = forbehandling.sak.id,
            brevId = forbehandling.brevId,
            brukerTokenInfo = bruker,
        )
    }

    private suspend fun utledBrevInnholdData(forbehandling: EtteroppgjoerForbehandling): EtteroppgjoerBrevData.Forhaandsvarsel =
        coroutineScope {
            EtteroppgjoerBrevData.Forhaandsvarsel(
                bosattUtland = false,
                norskInntekt = false,
                etteroppgjoersAar = 0,
                rettsgebyrBeloep = 0,
                resultatType = "",
                inntekt = 0,
                faktiskInntekt = 0,
                avviksBeloep = 0,
            )
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
