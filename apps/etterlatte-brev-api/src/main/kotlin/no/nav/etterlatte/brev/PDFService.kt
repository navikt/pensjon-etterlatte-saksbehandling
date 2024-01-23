package no.nav.etterlatte.brev

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.virusskanning.VirusScanRequest
import no.nav.etterlatte.brev.virusskanning.VirusScanService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory

class PDFService(private val db: BrevRepository, private val virusScanService: VirusScanService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun lagreOpplastaPDF(
        sakId: Long,
        multiPart: List<PartData>,
    ): Brev {
        val request =
            multiPart
                .first { it is PartData.FormItem }
                .let { it as PartData.FormItem }.value
                .let { objectMapper.readValue<BrevFraOpplastningRequest>(it) }

        val fil: ByteArray =
            multiPart
                .first { it is PartData.FileItem }
                .let { it as PartData.FileItem }
                .streamProvider()
                .readBytes()

        if (virusScanService.vedleggContainsVirus(VirusScanRequest(request, fil))) {
            logger.warn(
                "Filopplastinga er avvist fordi fila potensielt kan inneholde virus {}",
                request,
            )
        }

        return lagrePdf(sakId, fil, request.innhold, request.sak)
    }

    private fun lagrePdf(
        sakId: Long,
        fil: ByteArray,
        innhold: BrevInnhold,
        sak: Sak,
    ): Brev {
        val brev =
            db.opprettBrev(
                OpprettNyttBrev(
                    sakId = sakId,
                    behandlingId = null,
                    soekerFnr = sak.ident,
                    prosessType = BrevProsessType.OPPLASTET_PDF,
                    mottaker = Mottaker.tom(Folkeregisteridentifikator.of(sak.ident)),
                    opprettet = Tidspunkt.now(),
                    innhold = innhold,
                    innholdVedlegg = null,
                ),
            )

        db.lagrePdf(brev.id, Pdf(fil))

        return brev
    }
}
