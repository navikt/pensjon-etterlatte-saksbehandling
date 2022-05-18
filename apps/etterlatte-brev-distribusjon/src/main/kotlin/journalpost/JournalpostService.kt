package journalpost

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.journalpost.JournalpostKlient
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.journalpost.DokumentVariant
import no.nav.etterlatte.libs.common.journalpost.JournalPostType
import no.nav.etterlatte.libs.common.journalpost.JournalpostDokument
import no.nav.etterlatte.libs.common.journalpost.JournalpostRequest
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import org.slf4j.LoggerFactory

class JournalpostService(private val client: JournalpostKlient) {
    private val logger = LoggerFactory.getLogger(JournalpostService::class.java)

    fun journalfoer(melding: DistribusjonMelding): JournalpostResponse = runBlocking {
        logger.info("Oppretter journalpost for vedtak (id=${melding.vedtakId})")

        val dokumenter = emptyList<DokumentVariant>()
        val request = mapTilJournalpostRequest(melding, dokumenter)

        client.opprettJournalpost(request, true)
    }

    companion object {
        fun mapTilJournalpostRequest(
            melding: DistribusjonMelding,
            dokumenter: List<DokumentVariant>
        ): JournalpostRequest = JournalpostRequest(
            tittel = melding.tittel,
            journalpostType = JournalPostType.UTGAAENDE,
            behandlingstema = "ab0255", //SoeknadType.BARNEPENSJON
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
