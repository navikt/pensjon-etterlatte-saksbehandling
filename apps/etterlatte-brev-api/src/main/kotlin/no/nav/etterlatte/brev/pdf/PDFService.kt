package no.nav.etterlatte.brev.pdf

import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataFerdigstillingRequest
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevkodeRequest
import no.nav.etterlatte.brev.model.MottakerType
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.PdfMedData
import no.nav.etterlatte.brev.model.tomMottaker
import no.nav.etterlatte.brev.virusskanning.VirusScanRequest
import no.nav.etterlatte.brev.virusskanning.VirusScanService
import no.nav.etterlatte.brev.virusskanning.filErForStor
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class PDFService(
    private val db: BrevRepository,
    private val virusScanService: VirusScanService,
    private val pdfGenerator: PDFGenerator,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun ferdigstillOgGenererPDF(
        id: BrevID,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, ForenkletVedtak?, Enhetsnummer) -> AvsenderRequest,
        brevKodeMapping: (BrevkodeRequest) -> Brevkoder,
        brevDataMapping: suspend (BrevDataFerdigstillingRequest) -> BrevDataFerdigstilling,
    ): Pdf = pdfGenerator.ferdigstillOgGenererPDF(id, bruker, avsenderRequest, brevKodeMapping, brevDataMapping)

    fun hentPdfMedData(id: BrevID): PdfMedData? = db.hentPdfMedData(id)

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
        avsenderRequest: (BrukerTokenInfo, ForenkletVedtak?, Enhetsnummer) -> AvsenderRequest,
        brevKodeMapping: (BrevkodeRequest) -> Brevkoder,
        brevDataMapping: suspend (BrevDataFerdigstillingRequest) -> BrevDataFerdigstilling,
    ): Pdf = pdfGenerator.genererPdf(id, bruker, avsenderRequest, brevKodeMapping, brevDataMapping)

    suspend fun lagreOpplastaPDF(
        sakId: SakId,
        fil: ByteArray,
        request: BrevFraOpplastningRequest,
        bruker: BrukerTokenInfo,
    ): Result<Brev> {
        if (fil.isEmpty()) {
            throw UgyldigForespoerselException("MOTTOK_TOM_FIL", "Filen ble ikke lastet opp riktig. Prøv å laste opp filen på nytt.")
        }
        if (filErForStor(fil)) {
            logger.warn("Filopplastinga er avvist fordi fila er for stor $request")
            return Result.failure(IllegalArgumentException("Fila ${request.innhold.tittel} er større enn hva vi takler"))
        }

        if (virusScanService.filHarVirus(VirusScanRequest(request.innhold.tittel, fil))) {
            logger.warn("Filopplastinga er avvist fordi fila potensielt kan inneholde virus $request")
            return Result.failure(IllegalArgumentException("Virussjekken feila for ${request.innhold.tittel}"))
        }

        return Result.success(lagrePdf(sakId, fil, request.innhold, request.sak, bruker))
    }

    private fun lagrePdf(
        sakId: SakId,
        fil: ByteArray,
        innhold: BrevInnhold,
        sak: Sak,
        bruker: BrukerTokenInfo,
    ): Brev {
        val brev =
            db.opprettBrev(
                OpprettNyttBrev(
                    sakId = sakId,
                    behandlingId = null,
                    soekerFnr = sak.ident,
                    prosessType = BrevProsessType.OPPLASTET_PDF,
                    mottakere = listOf(tomMottaker(Folkeregisteridentifikator.of(sak.ident), MottakerType.HOVED)),
                    opprettet = Tidspunkt.now(),
                    innhold = innhold,
                    innholdVedlegg = null,
                    brevtype = Brevtype.OPPLASTET_PDF,
                    brevkoder = Brevkoder.OPPLASTET_PDF,
                ),
                bruker,
            )

        db.lagrePdf(brev.id, Pdf(fil))

        return brev
    }
}

data class BrevFraOpplastningRequest(
    val innhold: BrevInnhold,
    val sak: Sak,
)
