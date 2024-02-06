package no.nav.etterlatte.brev.varselbrev

import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Brevtype
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
    ): Brev {
        val brevkode =
            if (behandlingKlient.hentSak(sakId, brukerTokenInfo).sakType == SakType.BARNEPENSJON) {
                Brevkoder.BP_VARSEL
            } else {
                Brevkoder.OMS_VARSEL
            }

        return brevoppretter.opprettBrev(
            sakId = sakId,
            behandlingId = behandlingId,
            bruker = brukerTokenInfo,
            brevKode = brevkode.redigering,
            brevtype = Brevtype.VARSEL,
        ).first
    }

    suspend fun genererPdf(
        brevId: Long,
        bruker: BrukerTokenInfo,
    ) = pdfGenerator.genererPdf(
        id = brevId,
        bruker = bruker,
        avsenderRequest = { brukerToken, generellBrevData -> generellBrevData.avsenderRequest(brukerToken) },
        brevKode = { _ -> Brevkoder.BP_VARSEL },
        // TODO: Brevkode her kan også vera OMS_VARSEL i OMS-saker. Generelt kjens brevkodemappinga ut som noko som
        // fortener litt opprydding snart.
    )
}
