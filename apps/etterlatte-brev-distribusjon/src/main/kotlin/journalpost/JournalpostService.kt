package journalpost

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.journalpost.JournalpostKlient
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.journalpost.*
import org.slf4j.LoggerFactory

interface JournalpostService {
    fun journalfoer(melding: DistribusjonMelding): JournalpostResponse
}


class JournalpostServiceImpl(private val client: JournalpostKlient, private val brevService: BrevService) :
    JournalpostService {
    private val logger = LoggerFactory.getLogger(JournalpostService::class.java)

    override fun journalfoer(melding: DistribusjonMelding): JournalpostResponse = runBlocking {
        logger.info("Oppretter journalpost for brev med id=${melding.brevId}")

        val brevDokument = brevService.hentBrevInnhold(melding.brevId)
        val request = mapTilJournalpostRequest(melding, brevDokument)

        client.opprettJournalpost(request, true)
    }

    companion object {
        private const val BEHANDLINGSTEMA_BP = "ab0255"

        fun mapTilJournalpostRequest(
            melding: DistribusjonMelding,
            dokumentInnhold: ByteArray
        ): JournalpostRequest = JournalpostRequest(
            tittel = melding.tittel,
            journalpostType = JournalPostType.UTGAAENDE,
            behandlingstema = BEHANDLINGSTEMA_BP,
            avsenderMottaker = melding.mottaker,
            bruker = melding.bruker,
            eksternReferanseId = "${melding.vedtakId}.${melding.brevId}",
            // fagsaksystem = "EY??"
            // sak = {...}
            dokumenter = listOf(dokumentInnhold.tilJournalpostDokument(melding)),
            tema = "EYB", // https://confluence.adeo.no/display/BOA/Tema,
            kanal = "S", // skal denne legges til etterpå?
            journalfoerendeEnhet = "XX" // må hentes fra vedtak?
        )
    }
}

private fun ByteArray.tilJournalpostDokument(melding: DistribusjonMelding) = JournalpostDokument(
    tittel = melding.tittel,
    dokumentKategori = null, // depricated
    brevkode = "XX.YY-ZZ", // fra vedtak?
    dokumentvarianter = listOf(DokumentVariant.ArkivPDF(this.toString()))
)
