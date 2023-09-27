package no.nav.etterlatte.brev.dokarkiv

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.journalpost.AvsenderMottaker
import no.nav.etterlatte.brev.journalpost.Bruker
import no.nav.etterlatte.brev.journalpost.DokumentVariant
import no.nav.etterlatte.brev.journalpost.FerdigstillJournalpostRequest
import no.nav.etterlatte.brev.journalpost.JournalPostType
import no.nav.etterlatte.brev.journalpost.JournalpostDokument
import no.nav.etterlatte.brev.journalpost.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.brev.journalpost.JournalpostRequest
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.journalpost.JournalpostSak
import no.nav.etterlatte.brev.journalpost.Sakstype
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import org.slf4j.LoggerFactory
import java.util.Base64

interface DokarkivService {
    fun journalfoer(
        brevId: BrevID,
        vedtak: VedtakTilJournalfoering,
    ): JournalpostResponse

    fun journalfoer(
        brev: Brev,
        sak: Sak,
    ): JournalpostResponse

    fun ferdigstill(
        journalpostId: String,
        request: FerdigstillJournalpostRequest,
    )
}

class DokarkivServiceImpl(
    private val client: DokarkivKlient,
    private val db: BrevRepository,
) : DokarkivService {
    private val logger = LoggerFactory.getLogger(DokarkivService::class.java)

    override fun journalfoer(
        brevId: BrevID,
        vedtak: VedtakTilJournalfoering,
    ): JournalpostResponse =
        runBlocking {
            logger.info("Oppretter journalpost for brev med id=$brevId")

            val request = mapTilJournalpostRequest(brevId, vedtak)

            client.opprettJournalpost(request, true).also {
                logger.info("Journalpost opprettet (journalpostId=${it.journalpostId}, status=${it.journalpoststatus})")
            }
        }

    override fun journalfoer(
        brev: Brev,
        sak: Sak,
    ): JournalpostResponse =
        runBlocking {
            logger.info("Oppretter journalpost for brev med id=${brev.id}")

            val request = mapTilJournalpostRequest(brev, sak)

            client.opprettJournalpost(request, true).also {
                logger.info("Journalpost opprettet (journalpostId=${it.journalpostId}, status=${it.journalpoststatus})")
            }
        }

    override fun ferdigstill(
        journalpostId: String,
        request: FerdigstillJournalpostRequest,
    ) {
        runBlocking {
            client.ferdigstillJournalpost(journalpostId, request).also {
                logger.info("Journalpost med id=$journalpostId ferdigstilt: \n$it")
            }
        }
    }

    private fun mapTilJournalpostRequest(
        brevId: BrevID,
        vedtak: VedtakTilJournalfoering,
    ): JournalpostRequest {
        val innhold = requireNotNull(db.hentBrevInnhold(brevId))
        val pdf = requireNotNull(db.hentPdf(brevId))

        return JournalpostRequest(
            tittel = innhold.tittel,
            journalpostType = JournalPostType.UTGAAENDE,
            avsenderMottaker = AvsenderMottaker(vedtak.sak.ident),
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
