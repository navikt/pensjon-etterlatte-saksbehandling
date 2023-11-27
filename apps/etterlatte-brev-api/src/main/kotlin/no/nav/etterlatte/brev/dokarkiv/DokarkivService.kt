package no.nav.etterlatte.brev.dokarkiv

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.journalpost.AvsenderMottaker
import no.nav.etterlatte.brev.journalpost.Bruker
import no.nav.etterlatte.brev.journalpost.DokumentVariant
import no.nav.etterlatte.brev.journalpost.JournalPostType
import no.nav.etterlatte.brev.journalpost.JournalpostDokument
import no.nav.etterlatte.brev.journalpost.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.brev.journalpost.JournalpostRequest
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.journalpost.JournalpostSak
import no.nav.etterlatte.brev.journalpost.Sakstype
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import org.slf4j.LoggerFactory
import java.util.Base64

interface DokarkivService {
    suspend fun journalfoer(
        brev: Brev,
        vedtak: VedtakTilJournalfoering,
    ): JournalpostResponse

    suspend fun journalfoer(
        brev: Brev,
        sak: Sak,
    ): JournalpostResponse

    suspend fun ferdigstill(
        journalpostId: String,
        sak: Sak,
    )

    suspend fun endreTema(
        journalpostId: String,
        nyttTema: String,
    )
}

class DokarkivServiceImpl(
    private val client: DokarkivKlient,
    private val db: BrevRepository,
) : DokarkivService {
    private val logger = LoggerFactory.getLogger(DokarkivService::class.java)

    override suspend fun journalfoer(
        brev: Brev,
        vedtak: VedtakTilJournalfoering,
    ): JournalpostResponse {
        logger.info("Oppretter journalpost for brev med id=${brev.id}")

        val request = mapTilJournalpostRequest(brev, vedtak)

        return client.opprettJournalpost(request, true).also {
            logger.info("Journalpost opprettet (journalpostId=${it.journalpostId}, status=${it.journalpoststatus})")
        }
    }

    override suspend fun journalfoer(
        brev: Brev,
        sak: Sak,
    ): JournalpostResponse {
        logger.info("Oppretter journalpost for brev med id=${brev.id}")

        val request = mapTilJournalpostRequest(brev, sak)

        return client.opprettJournalpost(request, true).also {
            logger.info("Journalpost opprettet (journalpostId=${it.journalpostId}, status=${it.journalpoststatus})")
        }
    }

    override suspend fun ferdigstill(
        journalpostId: String,
        sak: Sak,
    ) {
        val request =
            OppdaterJournalpostSakRequest(
                bruker = Bruker(id = sak.ident),
                sak =
                    JournalpostSak(
                        sakstype = Sakstype.FAGSAK,
                        fagsakId = sak.id.toString(),
                        tema = sak.sakType.tema,
                    ),
            )

        client.oppdaterFagsak(journalpostId, request)
        client.ferdigstillJournalpost(journalpostId, sak.enhet)

        logger.info("Journalpost med id=$journalpostId ferdigstilt")
    }

    override suspend fun endreTema(
        journalpostId: String,
        nyttTema: String,
    ) {
        client.endreTema(journalpostId, nyttTema)
    }

    private fun mapTilJournalpostRequest(
        brev: Brev,
        vedtak: VedtakTilJournalfoering,
    ): JournalpostRequest {
        val brevId = brev.id
        val innhold = requireNotNull(db.hentBrevInnhold(brevId))
        val pdf = requireNotNull(db.hentPdf(brevId))

        val avsenderMottaker =
            if (vedtak.erMigrering) {
                AvsenderMottaker(id = vedtak.sak.ident, navn = "${brev.mottaker.navn} ved verge")
            } else {
                AvsenderMottaker(id = vedtak.sak.ident)
            }

        return JournalpostRequest(
            tittel = innhold.tittel,
            journalpostType = JournalPostType.UTGAAENDE,
            avsenderMottaker = avsenderMottaker,
            bruker = Bruker(vedtak.sak.ident),
            eksternReferanseId = "${vedtak.behandlingId}.$brevId",
            sak = JournalpostSak(Sakstype.FAGSAK, vedtak.sak.id.toString()),
            dokumenter = listOf(pdf.tilJournalpostDokument(innhold.tittel)),
            tema = vedtak.sak.sakType.tema, // https://confluence.adeo.no/display/BOA/Tema
            kanal = "S", // https://confluence.adeo.no/display/BOA/Utsendingskanal
            journalfoerendeEnhet = vedtak.ansvarligEnhet,
        )
    }

    private fun mapTilJournalpostRequest(
        brev: Brev,
        sak: Sak,
    ): JournalpostRequest {
        val innhold = requireNotNull(db.hentBrevInnhold(brev.id))
        val pdf = requireNotNull(db.hentPdf(brev.id))

        val mottaker =
            requireNotNull(brev.mottaker) {
                "Mottaker er 'null' i brev med id=${brev.id}"
            }
        val ident =
            requireNotNull(mottaker.foedselsnummer?.value ?: mottaker.orgnummer) {
                "Mottaker mangler både fnr. og orgnr. i brev med id=${brev.id}"
            }

        return JournalpostRequest(
            tittel = innhold.tittel,
            journalpostType = JournalPostType.UTGAAENDE,
            avsenderMottaker = AvsenderMottaker(ident),
            bruker = Bruker(brev.soekerFnr),
            eksternReferanseId = "${brev.sakId}.${brev.id}",
            sak = JournalpostSak(Sakstype.FAGSAK, brev.sakId.toString()),
            dokumenter = listOf(pdf.tilJournalpostDokument(innhold.tittel)),
            tema = sak.sakType.tema, // https://confluence.adeo.no/display/BOA/Tema
            kanal = "S", // https://confluence.adeo.no/display/BOA/Utsendingskanal
            journalfoerendeEnhet = sak.enhet,
        )
    }

    private fun Pdf.tilJournalpostDokument(tittel: String) =
        JournalpostDokument(
            tittel,
            brevkode = BREV_KODE,
            dokumentvarianter = listOf(DokumentVariant.ArkivPDF(Base64.getEncoder().encodeToString(bytes))),
        )
}
