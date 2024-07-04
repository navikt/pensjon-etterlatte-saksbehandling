package no.nav.etterlatte.brev.varselbrev

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.avsender
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

internal class VarselbrevService(
    private val db: BrevRepository,
    private val brevoppretter: Brevoppretter,
    private val behandlingService: BehandlingService,
    private val pdfGenerator: PDFGenerator,
    private val brevDataMapperFerdigstillVarsel: BrevDataMapperFerdigstillVarsel,
) {
    fun hentVarselbrev(behandlingId: UUID) = db.hentBrevForBehandling(behandlingId, Brevtype.VARSEL)

    suspend fun opprettVarselbrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        val sakType = behandlingService.hentSak(sakId, brukerTokenInfo).sakType
        val brevkode = hentBrevkode(sakType, behandlingId, brukerTokenInfo)

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
                    it.utlandstilknytningType,
                    it.revurderingaarsak,
                )
            }.first
    }

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

    suspend fun ferdigstillOgGenererPDF(
        brevId: BrevID,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, ForenkletVedtak?, String) -> AvsenderRequest =
            { brukerToken, vedtak, enhet -> avsender(brukerToken, vedtak, enhet) },
    ) = pdfGenerator.ferdigstillOgGenererPDF(
        id = brevId,
        bruker = bruker,
        avsenderRequest = avsenderRequest,
        brevKode = {
            val brev = db.hentBrev(brevId)
            runBlocking { hentBrevkode(it.sakType, brev.behandlingId, bruker) }
        },
        brevData = { brevDataMapperFerdigstillVarsel.hentBrevDataFerdigstilling(it) },
    )

    suspend fun genererPdf(
        brevId: Long,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, ForenkletVedtak?, String) -> AvsenderRequest =
            { brukerToken, forenkletVedtak, enhet -> avsender(brukerToken, forenkletVedtak, enhet) },
    ) = pdfGenerator.genererPdf(
        id = brevId,
        bruker = bruker,
        avsenderRequest = avsenderRequest,
        brevKode = {
            val brev = db.hentBrev(brevId)
            runBlocking { hentBrevkode(it.sakType, brev.behandlingId, bruker) }
        },
        brevData = { brevDataMapperFerdigstillVarsel.hentBrevDataFerdigstilling(it) },
    )
}
