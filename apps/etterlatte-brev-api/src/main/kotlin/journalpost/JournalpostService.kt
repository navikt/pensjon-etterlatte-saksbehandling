package journalpost

import model.Vedtak
import no.nav.etterlatte.db.Brev
import no.nav.etterlatte.journalpost.JournalpostKlient
import no.nav.etterlatte.libs.common.journalpost.AvsenderMottaker
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.journalpost.DokumentVariant
import no.nav.etterlatte.libs.common.journalpost.JournalPostType
import no.nav.etterlatte.libs.common.journalpost.JournalpostDokument
import no.nav.etterlatte.libs.common.journalpost.JournalpostRequest
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory
import java.util.*

class JournalpostService(private val client: JournalpostKlient) {
    private val logger = LoggerFactory.getLogger(JournalpostService::class.java)

    suspend fun journalfoer(vedtak: Vedtak, brev: Brev): JournalpostResponse {
        logger.info("Oppretter journalpost for vedtak (id=${vedtak.vedtakId}) med saksnummer ${vedtak.saksnummer}")

        val request = mapTilJournalpostRequest(vedtak, brev)

        return client.opprettJournalpost(request, true)
    }

    companion object {
        private val encoder = Base64.getEncoder()

        fun mapTilJournalpostRequest(vedtak: Vedtak, brev: Brev): JournalpostRequest = JournalpostRequest(
            tittel = "Vi har innvilget din søknad om barnepensjon",
            journalpostType = JournalPostType.UTGAAENDE,
            behandlingstema = "ab0255", //SoeknadType.BARNEPENSJON
            avsenderMottaker = AvsenderMottaker(id = vedtak.barn.fnr),
            bruker = Bruker(id = vedtak.barn.fnr),
            eksternReferanseId = "todo", // blir sjekket for duplikat.
            // fagsaksystem = "EY??"
            // sak = {...}
            dokumenter = listOf(
                JournalpostDokument(
                    tittel = "Vi har innvilget din søknad om barnepensjon",
                    // "brevkode" = "??"
                    dokumentKategori = null, // depricated
                    brevkode = "XX.YY-ZZ",
                    dokumentvarianter = listOf(
                        DokumentVariant.ArkivPDF(encoder.encodeToString(brev.data)),
                        DokumentVariant.OriginalJson(vedtak.toJson()) // ??
                    )
                )
            ),
            tema = "EYB", // https://confluence.adeo.no/display/BOA/Tema,
            kanal = "S", // skal denne legges til etterpå?
            journalfoerendeEnhet = "XX" // må hentes fra vedtak?
        )
    }
}
