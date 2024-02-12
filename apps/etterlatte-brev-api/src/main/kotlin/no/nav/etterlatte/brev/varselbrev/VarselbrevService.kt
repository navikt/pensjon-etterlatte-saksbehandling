package no.nav.etterlatte.brev.varselbrev

import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Brevtype
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

internal class VarselbrevService(
    private val db: BrevRepository,
    private val brevoppretter: Brevoppretter,
    private val behandlingKlient: BehandlingKlient,
    private val pdfGenerator: PDFGenerator,
) {
    fun hentVarselbrev(behandlingId: UUID) = db.hentBrevForBehandling(behandlingId, Brevtype.VARSEL)

    suspend fun opprettVarselbrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VarselbrevResponse {
        val brevkode = hentBrevkodeForSak(sakId, brukerTokenInfo)

        return brevoppretter.opprettBrev(
            sakId = sakId,
            behandlingId = behandlingId,
            bruker = brukerTokenInfo,
            brevKode = { brevkode.redigering },
            brevtype = Brevtype.VARSEL,
        ) { ManueltBrevData() }.let {
            VarselbrevResponse(it.first, it.second, brevkode)
        }
    }

    private suspend fun hentBrevkodeForSak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) = hentBrevkode(behandlingKlient.hentSak(sakId, brukerTokenInfo).sakType)

    private fun hentBrevkode(sakType: SakType) =
        if (sakType == SakType.BARNEPENSJON) {
            Brevkoder.BP_VARSEL
        } else {
            Brevkoder.OMS_VARSEL
        }

    suspend fun genererPdf(
        brevId: Long,
        bruker: BrukerTokenInfo,
    ) = pdfGenerator.genererPdf(
        id = brevId,
        bruker = bruker,
        automatiskMigreringRequest = null,
        avsenderRequest = { brukerToken, generellBrevData -> generellBrevData.avsenderRequest(brukerToken) },
        brevKode = { hentBrevkode(it.sakType) },
        brevData = { ManueltBrevData(it.innholdMedVedlegg.innhold()) },
    )
}

data class VarselbrevResponse(val brev: Brev, val generellBrevData: GenerellBrevData, val brevkoder: Brevkoder)
