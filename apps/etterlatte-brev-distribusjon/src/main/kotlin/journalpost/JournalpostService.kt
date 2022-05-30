package journalpost

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.journalpost.JournalpostKlient
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.journalpost.*
import org.slf4j.LoggerFactory

interface JournalpostService {
    fun journalfoer(melding: DistribusjonMelding): JournalpostResponse
}


class JournalpostServiceImpl(private val client: JournalpostKlient) : JournalpostService {
    private val logger = LoggerFactory.getLogger(JournalpostService::class.java)

    override fun journalfoer(melding: DistribusjonMelding): JournalpostResponse = runBlocking {
        logger.info("Oppretter journalpost for brev med id=${melding.brevId}")

        val dokumenter = emptyList<DokumentVariant>()
        val request = mapTilJournalpostRequest(melding, dokumenter)

        client.opprettJournalpost(request, true)
    }

    companion object {
        private const val BEHANDLINGSTEMA_BP = "ab0255"

        fun mapTilJournalpostRequest(
            melding: DistribusjonMelding,
            dokumenter: List<DokumentVariant>
        ): JournalpostRequest = JournalpostRequest(
            tittel = melding.tittel,
            journalpostType = JournalPostType.UTGAAENDE,
            behandlingstema = BEHANDLINGSTEMA_BP,
            avsenderMottaker = melding.mottaker,
            bruker = melding.bruker,
            eksternReferanseId = "todo", // blir sjekket for duplikat.
            // fagsaksystem = "EY??"
            // sak = {...}
            dokumenter = listOf(
                JournalpostDokument(
                    tittel = "Vi har innvilget din søknad om barnepensjon",
                    // "brevkode" = "??"
                    dokumentKategori = null, // depricated
                    brevkode = "XX.YY-ZZ",
                    dokumentvarianter = dokumenter
                )
            ),
            tema = "EYB", // https://confluence.adeo.no/display/BOA/Tema,
            kanal = "S", // skal denne legges til etterpå?
            journalfoerendeEnhet = "XX" // må hentes fra vedtak?
        )
    }
}
