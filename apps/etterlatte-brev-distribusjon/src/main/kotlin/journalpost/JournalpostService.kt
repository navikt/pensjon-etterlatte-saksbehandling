package journalpost

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.journalpost.JournalpostKlient
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.journalpost.AvsenderMottaker
import no.nav.etterlatte.libs.common.journalpost.DokumentVariant
import no.nav.etterlatte.libs.common.journalpost.JournalPostType
import no.nav.etterlatte.libs.common.journalpost.JournalpostDokument
import no.nav.etterlatte.libs.common.journalpost.JournalpostRequest
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import org.slf4j.LoggerFactory
import java.util.*

interface JournalpostService {
    fun journalfoer(melding: DistribusjonMelding): JournalpostResponse
}

class JournalpostServiceImpl(
    private val client: JournalpostKlient,
    private val brevService: BrevService
) : JournalpostService {
    private val logger = LoggerFactory.getLogger(JournalpostService::class.java)

    override fun journalfoer(melding: DistribusjonMelding): JournalpostResponse = runBlocking {
        logger.info("Oppretter journalpost for brev med id=${melding.brevId}")

        val brevDokument = brevService.hentBrevInnhold(melding.brevId)
        logger.info("Oppretter journalpost med brevinnhold", brevDokument)

        val request = mapTilJournalpostRequest(melding, brevDokument)
        logger.info("Oppretter journalpost med request", brevDokument)

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
            avsenderMottaker = melding.mottaker.tilAvsenderMottaker(),
            bruker = melding.bruker,
            eksternReferanseId = "${melding.behandlingId}.${melding.brevId}",
            // sak = {...}
            dokumenter = listOf(dokumentInnhold.tilJournalpostDokument(melding)),
            tema = "EYB", // https://confluence.adeo.no/display/BOA/Tema,
            kanal = "S", // https://confluence.adeo.no/display/BOA/Utsendingskanal
            journalfoerendeEnhet = melding.journalfoerendeEnhet
        )
    }
}

private fun Mottaker.tilAvsenderMottaker() = when {
    foedselsnummer != null -> AvsenderMottaker(id = foedselsnummer!!.value)
    orgnummer != null -> AvsenderMottaker(id = orgnummer, idType = "ORGNR")
    adresse != null -> {
        val navn = "${adresse!!.fornavn} ${adresse!!.etternavn}"
        AvsenderMottaker(id = null, navn = navn, idType = null, land = adresse!!.land)
    }
    else -> throw Exception("Ingen brevmottaker spesifisert")
}

private fun ByteArray.tilJournalpostDokument(melding: DistribusjonMelding) = JournalpostDokument(
    tittel = melding.tittel,
    brevkode = melding.brevKode,
    dokumentvarianter = listOf(DokumentVariant.ArkivPDF(Base64.getEncoder().encodeToString(this)))
)