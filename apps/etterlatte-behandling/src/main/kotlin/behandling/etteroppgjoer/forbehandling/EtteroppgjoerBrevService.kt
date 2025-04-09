package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.etteroppgjoer.DetaljertForbehandlingDto
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
import no.nav.pensjon.brevbaker.api.model.Kroner
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
        val (detaljertForbehandling, brevInnholdData) = hentDetaljertForbehandlingOgBrevInnhold(behandlingId, brukerTokenInfo)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = detaljertForbehandling.behandling.sak,
                    brevInnholdData = brevInnholdData,
                    brevRedigerbarInnholdData = null,
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
        val (detaljertForbehandling, brevInnholdData) = hentDetaljertForbehandlingOgBrevInnhold(behandlingId, brukerTokenInfo)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = detaljertForbehandling.behandling.sak,
                    brevInnholdData = brevInnholdData,
                    skalLagres = false,
                    brukerTokenInfo = brukerTokenInfo,
                    brevRedigerbarInnholdData = null,
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
        val (detaljertForbehandling, brevInnholdData) = hentDetaljertForbehandlingOgBrevInnhold(behandlingId, brukerTokenInfo)
        brevKlient.ferdigstillStrukturertBrev(behandlingId, brevInnholdData.brevKode.brevtype, brukerTokenInfo)
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf {
        val (detaljertForbehandling, brevInnholdData) = hentDetaljertForbehandlingOgBrevInnhold(behandlingId, brukerTokenInfo)
        val request =
            retryOgPakkUt {
                utledBrevRequest(
                    sak = detaljertForbehandling.behandling.sak,
                    brevInnholdData = brevInnholdData,
                    brevRedigerbarInnholdData = null,
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

    private suspend fun hentDetaljertForbehandlingOgBrevInnhold(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pair<DetaljertForbehandlingDto, EtteroppgjoerBrevData.Forhaandsvarsel> {
        val detaljertForbehandling =
            etteroppgjoerForbehandlingService.hentDetaljertForbehandling(
                behandlingId,
                brukerTokenInfo,
            )

        val brevInnholdData = utledBrevInnholdData(detaljertForbehandling)

        return Pair(detaljertForbehandling, brevInnholdData)
    }

    private suspend fun utledBrevInnholdData(data: DetaljertForbehandlingDto): EtteroppgjoerBrevData.Forhaandsvarsel =
        coroutineScope {
            EtteroppgjoerBrevData.Forhaandsvarsel(
                bosattUtland = false, // TODO
                norskInntekt = false, // TODO
                etteroppgjoersAar = data.behandling.aar,
                rettsgebyrBeloep = Kroner(1234), // TODO hent faktisk rettsgebyr
                resultatType = data.beregnetEtteroppgjoerResultat.resultatType,
                inntekt = Kroner(data.beregnetEtteroppgjoerResultat.utbetaltStoenad.toInt()), // TODO long
                faktiskInntekt = Kroner(data.beregnetEtteroppgjoerResultat.nyBruttoStoenad.toInt()), // TODO long
                avviksBeloep = Kroner(data.beregnetEtteroppgjoerResultat.differanse.toInt()), // TODO long
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
