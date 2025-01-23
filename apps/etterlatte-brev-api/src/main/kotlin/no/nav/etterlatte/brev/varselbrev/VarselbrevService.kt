package no.nav.etterlatte.brev.varselbrev

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.opprettAvsenderRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

internal class VarselbrevService(
    private val db: BrevRepository,
    private val brevoppretter: Brevoppretter,
    private val behandlingService: BehandlingService,
    private val pdfGenerator: PDFGenerator,
    private val brevDataMapperFerdigstillVarsel: BrevDataMapperFerdigstillVarsel,
    private val grunnlagKlient: GrunnlagKlient,
) {
    fun hentVarselbrev(behandlingId: UUID) = db.hentBrevForBehandling(behandlingId, Brevtype.VARSEL)

    suspend fun opprettVarselbrev(
        sakId: SakId,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        val sakType = behandlingService.hentSak(sakId, brukerTokenInfo).sakType
        val brevkode = hentBrevkode(sakType, behandlingId, brukerTokenInfo)
        val behandling = behandlingService.hentBehandling(behandlingId, brukerTokenInfo)
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.id, brukerTokenInfo)

        val brevdata =
            BrevDataMapperRedigerbartUtfallVarsel.hentBrevDataRedigerbar(
                sakType,
                brukerTokenInfo,
                behandling.utlandstilknytning?.type,
                behandling.revurderingsaarsak,
                grunnlag,
                behandling,
            )

        return brevoppretter
            .opprettBrevSomHarInnhold(
                sakId = sakId,
                behandlingId = behandlingId,
                bruker = brukerTokenInfo,
                brevKode = brevkode,
                brevData = brevdata,
            ).first
    }

    suspend fun ferdigstillOgGenererPDF(
        brevId: BrevID,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, ForenkletVedtak?, Enhetsnummer) -> AvsenderRequest =
            { brukerToken, vedtak, enhet -> opprettAvsenderRequest(brukerToken, vedtak, enhet) },
    ) = pdfGenerator.ferdigstillOgGenererPDF(
        id = brevId,
        bruker = bruker,
        avsenderRequest = avsenderRequest,
        brevKodeMapping = {
            val brev = db.hentBrev(brevId)
            runBlocking { hentBrevkode(it.sakType, brev.behandlingId, bruker) }
        },
        brevDataMapping = { brevDataMapperFerdigstillVarsel.hentBrevDataFerdigstilling(it) },
    )

    suspend fun genererPdfFerdigstilling(
        brevId: Long,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, ForenkletVedtak?, Enhetsnummer) -> AvsenderRequest =
            { brukerToken, vedtak, enhet -> opprettAvsenderRequest(brukerToken, vedtak, enhet) },
    ) = pdfGenerator.genererPdf(
        id = brevId,
        bruker = bruker,
        avsenderRequest = avsenderRequest,
        brevKodeMapping = {
            val brev = db.hentBrev(brevId)
            runBlocking { hentBrevkode(it.sakType, brev.behandlingId, bruker) }
        },
        brevDataMapping = { brevDataMapperFerdigstillVarsel.hentBrevDataFerdigstilling(it) },
    )

    private suspend fun hentBrevkode(
        sakType: SakType,
        behandlingId: UUID?,
        brukerTokenInfo: BrukerTokenInfo,
    ) = if (sakType == SakType.BARNEPENSJON) {
        Brevkoder.BP_VARSEL
    } else {
        val erAktivitetsplikt =
            behandlingId
                ?.let { behandlingService.hentBehandling(behandlingId, brukerTokenInfo) }
                ?.revurderingsaarsak == Revurderingaarsak.AKTIVITETSPLIKT

        if (erAktivitetsplikt) {
            Brevkoder.OMS_VARSEL_AKTIVITETSPLIKT
        } else {
            Brevkoder.OMS_VARSEL
        }
    }
}
