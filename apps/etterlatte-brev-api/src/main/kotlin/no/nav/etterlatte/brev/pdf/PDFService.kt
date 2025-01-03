package no.nav.etterlatte.brev.pdf

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.MottakerType
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.tomMottaker
import no.nav.etterlatte.brev.virusskanning.VirusScanRequest
import no.nav.etterlatte.brev.virusskanning.VirusScanService
import no.nav.etterlatte.brev.virusskanning.filErForStor
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class PDFService(
    private val db: BrevRepository,
    private val virusScanService: VirusScanService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun lagreOpplastaPDF(
        sakId: SakId,
        multiPart: List<PartData>,
        bruker: BrukerTokenInfo,
    ): Result<Brev> {
        val request =
            multiPart
                .first { it is PartData.FormItem }
                .let { it as PartData.FormItem }
                .value
                .let { objectMapper.readValue<BrevFraOpplastningRequest>(it) }

        val fil: ByteArray =
            multiPart
                .first { it is PartData.FileItem }
                .let { it as PartData.FileItem }
                .streamProvider()
                .readBytes()

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
