package no.nav.etterlatte.brev.varselbrev

import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

internal class VarselbrevService(
    private val db: BrevRepository,
    private val brevoppretter: Brevoppretter,
    private val behandlingKlient: BehandlingKlient,
    private val pdfGenerator: PDFGenerator,
    private val brevDataMapperFerdigstillVarsel: BrevDataMapperFerdigstillVarsel,
) {
    fun hentVarselbrev(behandlingId: UUID) = db.hentBrevForBehandling(behandlingId, Brevtype.VARSEL)

    suspend fun opprettVarselbrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VarselbrevResponse {
        val sakType = behandlingKlient.hentSak(sakId, brukerTokenInfo).sakType
        val brevkode = hentBrevkode(sakType)

        return brevoppretter
            .opprettBrev(
                sakId = sakId,
                behandlingId = behandlingId,
                bruker = brukerTokenInfo,
                brevKode = { brevkode.redigering },
                brevtype = Brevtype.VARSEL,
            ) {
                BrevDataMapperRedigerbartUtfallVarsel.hentBrevDataRedigerbar(
                    sakType,
                    brukerTokenInfo,
                    it.generellBrevData.utlandstilknytning,
                )
            }.let {
                VarselbrevResponse(it.first, it.second, brevkode)
            }
    }

    private fun hentBrevkode(sakType: SakType) =
        if (sakType == SakType.BARNEPENSJON) {
            Brevkoder.BP_VARSEL
        } else {
            Brevkoder.OMS_VARSEL
        }

    suspend fun ferdigstillOgGenererPDF(
        brevId: BrevID,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, GenerellBrevData) -> AvsenderRequest =
            { brukerToken, generellBrevData -> generellBrevData.avsenderRequest(brukerToken) },
    ) = pdfGenerator.ferdigstillOgGenererPDF(
        id = brevId,
        bruker = bruker,
        avsenderRequest = avsenderRequest,
        brevKode = { hentBrevkode(it.sakType) },
        brevData = { brevDataMapperFerdigstillVarsel.hentBrevDataFerdigstilling(it) },
    )

    suspend fun genererPdf(
        brevId: Long,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, GenerellBrevData) -> AvsenderRequest =
            { brukerToken, generellBrevData -> generellBrevData.avsenderRequest(brukerToken) },
    ) = pdfGenerator.genererPdf(
        id = brevId,
        bruker = bruker,
        avsenderRequest = avsenderRequest,
        brevKode = { hentBrevkode(it.sakType) },
        brevData = { brevDataMapperFerdigstillVarsel.hentBrevDataFerdigstilling(it) },
    )
}

data class VarselbrevResponse(
    val brev: Brev,
    val generellBrevData: GenerellBrevData,
    val brevkoder: Brevkoder,
)
