package no.nav.etterlatte.brev.dokarkiv

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.journalpost.AvsenderMottaker
import no.nav.etterlatte.brev.journalpost.Bruker
import no.nav.etterlatte.brev.journalpost.DokumentVariant
import no.nav.etterlatte.brev.journalpost.JournalPostType
import no.nav.etterlatte.brev.journalpost.JournalpostDokument
import no.nav.etterlatte.brev.journalpost.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.brev.journalpost.JournalpostRequest
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.journalpost.Sak
import no.nav.etterlatte.brev.journalpost.Sakstype
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import org.slf4j.LoggerFactory
import java.util.*

interface DokarkivService {
    fun journalfoer(vedtaksbrev: Brev, vedtak: VedtakTilJournalfoering): JournalpostResponse
}

class DokarkivServiceImpl(
    private val client: DokarkivKlient,
    private val db: BrevRepository
) : DokarkivService {
    private val logger = LoggerFactory.getLogger(DokarkivService::class.java)

    override fun journalfoer(vedtaksbrev: Brev, vedtak: VedtakTilJournalfoering): JournalpostResponse = runBlocking {
        logger.info("Oppretter journalpost for brev med id=${vedtaksbrev.id}")

        val innhold = requireNotNull(db.hentBrevInnhold(vedtaksbrev.id))

        val request = mapTilJournalpostRequest(vedtaksbrev, vedtak, innhold)

        client.opprettJournalpost(request, true).also {
            logger.info("Journalpost opprettet (journalpostId=${it.journalpostId}, status=${it.journalpoststatus})")
        }
    }

    private fun mapTilJournalpostRequest(
        vedtaksbrev: Brev,
        vedtak: VedtakTilJournalfoering,
        innhold: BrevInnhold
    ): JournalpostRequest {
        val pdf = requireNotNull(db.hentPdf(vedtaksbrev.id))

        return JournalpostRequest(
            tittel = innhold.tittel,
            journalpostType = JournalPostType.UTGAAENDE,
            avsenderMottaker = AvsenderMottaker(vedtak.sak.ident),
            bruker = Bruker(vedtak.sak.ident),
            eksternReferanseId = "${vedtaksbrev.behandlingId}.${vedtaksbrev.id}",
            sak = Sak(Sakstype.FAGSAK, vedtak.sak.id.toString()),
            dokumenter = listOf(pdf.tilJournalpostDokument(innhold.tittel)),
            tema = vedtak.sak.sakType.tema, // https://confluence.adeo.no/display/BOA/Tema
            kanal = "S", // https://confluence.adeo.no/display/BOA/Utsendingskanal
            journalfoerendeEnhet = vedtak.ansvarligEnhet
        )
    }

    private fun Pdf.tilJournalpostDokument(tittel: String) = JournalpostDokument(
        tittel,
        brevkode = BREV_KODE,
        dokumentvarianter = listOf(DokumentVariant.ArkivPDF(Base64.getEncoder().encodeToString(bytes)))
    )
}