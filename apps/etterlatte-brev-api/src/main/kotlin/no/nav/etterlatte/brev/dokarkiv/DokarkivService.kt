package no.nav.etterlatte.brev.dokarkiv

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.journalpost.AvsenderMottaker
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.journalpost.DokumentVariant
import no.nav.etterlatte.libs.common.journalpost.JournalPostType
import no.nav.etterlatte.libs.common.journalpost.JournalpostDokument
import no.nav.etterlatte.libs.common.journalpost.JournalpostKoder.Companion.BEHANDLINGSTEMA_BP
import no.nav.etterlatte.libs.common.journalpost.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.libs.common.journalpost.JournalpostRequest
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.journalpost.Sak
import no.nav.etterlatte.libs.common.journalpost.Sakstype
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import org.slf4j.LoggerFactory
import java.util.*

interface DokarkivService {
    fun journalfoer(vedtaksbrev: Brev, vedtak: VedtakDto): JournalpostResponse
}

class DokarkivServiceImpl(
    private val client: DokarkivKlient,
    private val db: BrevRepository
) : DokarkivService {
    private val logger = LoggerFactory.getLogger(DokarkivService::class.java)

    override fun journalfoer(vedtaksbrev: Brev, vedtak: VedtakDto): JournalpostResponse = runBlocking {
        logger.info("Oppretter journalpost for brev med id=${vedtaksbrev.id}")

        val innhold = db.hentBrevInnhold(vedtaksbrev.id)
        logger.info("Oppretter journalpost med brevinnhold", innhold)

        val request = mapTilJournalpostRequest(vedtaksbrev, vedtak, innhold.data)
        logger.info("Oppretter journalpost med request", innhold)

        client.opprettJournalpost(request, true)
    }

    private fun mapTilJournalpostRequest(
        vedtaksbrev: Brev,
        vedtak: VedtakDto,
        dokumentInnhold: ByteArray
    ): JournalpostRequest {
        return JournalpostRequest(
            tittel = vedtaksbrev.tittel,
            journalpostType = JournalPostType.UTGAAENDE,
            behandlingstema = BEHANDLINGSTEMA_BP,
            avsenderMottaker = vedtaksbrev.mottaker.tilAvsenderMottaker(),
            bruker = Bruker(vedtak.sak.ident),
            eksternReferanseId = "${vedtaksbrev.behandlingId}.${vedtaksbrev.id}",
            sak = Sak(Sakstype.FAGSAK, vedtaksbrev.behandlingId.toString()),
            dokumenter = listOf(dokumentInnhold.tilJournalpostDokument(vedtaksbrev.tittel)),
            tema = "EYB", // https://confluence.adeo.no/display/BOA/Tema
            kanal = "S", // https://confluence.adeo.no/display/BOA/Utsendingskanal
            journalfoerendeEnhet = vedtak.vedtakFattet!!.ansvarligEnhet
        )
    }

    private fun ByteArray.tilJournalpostDokument(tittel: String) = JournalpostDokument(
        tittel,
        brevkode = BREV_KODE,
        dokumentvarianter = listOf(DokumentVariant.ArkivPDF(Base64.getEncoder().encodeToString(this)))
    )

    private fun Mottaker.tilAvsenderMottaker() = when {
        foedselsnummer != null -> AvsenderMottaker(id = foedselsnummer!!.value)
        orgnummer != null -> AvsenderMottaker(id = orgnummer, idType = "ORGNR")
        adresse != null -> {
            AvsenderMottaker(id = null, navn = adresse!!.navn, idType = null, land = adresse!!.land)
        }
        else -> throw Exception("Ingen brevmottaker spesifisert")
    }
}