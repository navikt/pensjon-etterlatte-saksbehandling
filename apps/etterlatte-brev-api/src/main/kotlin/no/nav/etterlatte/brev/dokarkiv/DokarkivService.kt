package no.nav.etterlatte.brev.dokarkiv

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.journalpost.AvsenderMottaker
import no.nav.etterlatte.brev.journalpost.Bruker
import no.nav.etterlatte.brev.journalpost.DokumentVariant
import no.nav.etterlatte.brev.journalpost.JournalPostType
import no.nav.etterlatte.brev.journalpost.JournalpostDokument
import no.nav.etterlatte.brev.journalpost.JournalpostKoder.Companion.BEHANDLINGSTEMA_BP
import no.nav.etterlatte.brev.journalpost.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.brev.journalpost.JournalpostRequest
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.journalpost.Sak
import no.nav.etterlatte.brev.journalpost.Sakstype
import no.nav.etterlatte.brev.model.Brev
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

        val innhold = db.hentBrevInnhold(vedtaksbrev.id)
        logger.info("Oppretter journalpost med brevinnhold", innhold)

        val request = mapTilJournalpostRequest(vedtaksbrev, vedtak, innhold.data)
        logger.info("Oppretter journalpost med request", innhold)

        client.opprettJournalpost(request, true)
    }

    private fun mapTilJournalpostRequest(
        vedtaksbrev: Brev,
        vedtak: VedtakTilJournalfoering,
        dokumentInnhold: ByteArray
    ): JournalpostRequest {
        return JournalpostRequest(
            tittel = vedtaksbrev.tittel,
            journalpostType = JournalPostType.UTGAAENDE,
            behandlingstema = BEHANDLINGSTEMA_BP,
            avsenderMottaker = AvsenderMottaker(vedtak.soekerIdent),
            bruker = Bruker(vedtak.soekerIdent),
            eksternReferanseId = "${vedtaksbrev.behandlingId}.${vedtaksbrev.id}",
            sak = Sak(Sakstype.FAGSAK, vedtak.sakId.toString()),
            dokumenter = listOf(dokumentInnhold.tilJournalpostDokument(vedtaksbrev.tittel)),
            tema = "EYB", // https://confluence.adeo.no/display/BOA/Tema
            kanal = "S", // https://confluence.adeo.no/display/BOA/Utsendingskanal
            journalfoerendeEnhet = vedtak.ansvarligEnhet
        )
    }

    private fun ByteArray.tilJournalpostDokument(tittel: String) = JournalpostDokument(
        tittel,
        brevkode = BREV_KODE,
        dokumentvarianter = listOf(DokumentVariant.ArkivPDF(Base64.getEncoder().encodeToString(this)))
    )
}