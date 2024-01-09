package no.nav.etterlatte.brev.dokarkiv

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import org.slf4j.LoggerFactory
import java.util.Base64

interface DokarkivService {
    suspend fun journalfoer(
        brevId: BrevID,
        vedtak: VedtakTilJournalfoering,
    ): OpprettJournalpostResponse

    suspend fun journalfoer(
        brev: Brev,
        sak: Sak,
    ): OpprettJournalpostResponse

    suspend fun oppdater(
        journalpostId: String,
        forsoekFerdistill: Boolean,
        journalfoerendeEnhet: String?,
        request: OppdaterJournalpostRequest,
    ): OppdaterJournalpostResponse

    suspend fun feilregistrerSakstilknytning(journalpostId: String)

    suspend fun opphevFeilregistrertSakstilknytning(journalpostId: String)

    suspend fun knyttTilAnnenSak(
        journalpostId: String,
        request: KnyttTilAnnenSakRequest,
    ): KnyttTilAnnenSakResponse
}

class DokarkivServiceImpl(
    private val client: DokarkivKlient,
    private val db: BrevRepository,
) : DokarkivService {
    private val logger = LoggerFactory.getLogger(DokarkivService::class.java)

    override suspend fun journalfoer(
        brevId: BrevID,
        vedtak: VedtakTilJournalfoering,
    ): OpprettJournalpostResponse {
        logger.info("Oppretter journalpost for brev med id=$brevId")

        val request = mapTilJournalpostRequest(brevId, vedtak)

        return client.opprettJournalpost(request, true).also {
            logger.info(
                "Journalpost opprettet (journalpostId=${it.journalpostId}, ferdigstilt=${it.journalpostferdigstilt})",
            )
        }
    }

    override suspend fun journalfoer(
        brev: Brev,
        sak: Sak,
    ): OpprettJournalpostResponse {
        logger.info("Oppretter journalpost for brev med id=${brev.id}")

        val request = mapTilJournalpostRequest(brev, sak)

        return client.opprettJournalpost(request, true)
            .also {
                logger.info(
                    "Journalpost opprettet (journalpostId=${it.journalpostId}, ferdigstilt=${it.journalpostferdigstilt})",
                )
            }
    }

    override suspend fun oppdater(
        journalpostId: String,
        forsoekFerdistill: Boolean,
        journalfoerendeEnhet: String?,
        request: OppdaterJournalpostRequest,
    ): OppdaterJournalpostResponse {
        val response = client.oppdaterJournalpost(journalpostId, request)

        logger.info("Journalpost med id=$journalpostId oppdatert OK!")

        if (forsoekFerdistill) {
            if (journalfoerendeEnhet.isNullOrBlank()) {
                logger.error("Kan ikke ferdigstille journalpost=$journalpostId når enhet mangler")
            } else {
                val ferdigstilt = client.ferdigstillJournalpost(journalpostId, journalfoerendeEnhet)
                logger.info("journalpostId=$journalpostId, ferdigstillrespons='$ferdigstilt'")
            }
        }

        return response
    }

    override suspend fun feilregistrerSakstilknytning(journalpostId: String) {
        client.feilregistrerSakstilknytning(journalpostId)
    }

    override suspend fun opphevFeilregistrertSakstilknytning(journalpostId: String) {
        client.opphevFeilregistrertSakstilknytning(journalpostId)
    }

    override suspend fun knyttTilAnnenSak(
        journalpostId: String,
        request: KnyttTilAnnenSakRequest,
    ): KnyttTilAnnenSakResponse {
        return client.knyttTilAnnenSak(journalpostId, request).also {
            logger.info("Journalpost knyttet til annen sak (nyJournalpostId=${it.nyJournalpostId})\n$request")
        }
    }

    private fun mapTilJournalpostRequest(
        brevId: BrevID,
        vedtak: VedtakTilJournalfoering,
    ): OpprettJournalpostRequest {
        val innhold = requireNotNull(db.hentBrevInnhold(brevId))
        val pdf = requireNotNull(db.hentPdf(brevId))
        val brev = requireNotNull(db.hentBrev(brevId))

        return OpprettJournalpostRequest(
            tittel = innhold.tittel,
            journalposttype = JournalPostType.UTGAAENDE,
            avsenderMottaker = brev.avsenderMottaker(),
            bruker = Bruker(vedtak.sak.ident),
            eksternReferanseId = "${vedtak.behandlingId}.$brevId",
            sak = JournalpostSak(Sakstype.FAGSAK, vedtak.sak.id.toString()),
            dokumenter = listOf(pdf.tilJournalpostDokument(innhold.tittel)),
            tema = vedtak.sak.sakType.tema,
            kanal = "S",
            journalfoerendeEnhet = vedtak.ansvarligEnhet,
        )
    }

    private fun mapTilJournalpostRequest(
        brev: Brev,
        sak: Sak,
    ): OpprettJournalpostRequest {
        val innhold = requireNotNull(db.hentBrevInnhold(brev.id))
        val pdf = requireNotNull(db.hentPdf(brev.id))

        return OpprettJournalpostRequest(
            tittel = innhold.tittel,
            journalposttype = JournalPostType.UTGAAENDE,
            avsenderMottaker = brev.avsenderMottaker(),
            bruker = Bruker(brev.soekerFnr),
            eksternReferanseId = "${brev.sakId}.${brev.id}",
            sak = JournalpostSak(Sakstype.FAGSAK, brev.sakId.toString()),
            dokumenter = listOf(pdf.tilJournalpostDokument(innhold.tittel)),
            tema = sak.sakType.tema,
            kanal = "S",
            journalfoerendeEnhet = sak.enhet,
        )
    }

    private fun Pdf.tilJournalpostDokument(tittel: String) =
        JournalpostDokument(
            tittel,
            brevkode = BREV_KODE,
            dokumentvarianter = listOf(DokumentVariant.ArkivPDF(Base64.getEncoder().encodeToString(bytes))),
        )

    private fun Brev.avsenderMottaker(): AvsenderMottaker {
        return AvsenderMottaker(
            id = this.mottaker.foedselsnummer?.value ?: this.mottaker.orgnummer,
            idType =
                if (this.mottaker.foedselsnummer != null) {
                    "FNR"
                } else if (this.mottaker.orgnummer != null) {
                    "ORGNR"
                } else {
                    null
                },
            navn =
                if (this.mottaker.foedselsnummer?.value != null || this.mottaker.orgnummer != null) {
                    null
                } else {
                    this.mottaker.navn
                },
        )
    }
}
