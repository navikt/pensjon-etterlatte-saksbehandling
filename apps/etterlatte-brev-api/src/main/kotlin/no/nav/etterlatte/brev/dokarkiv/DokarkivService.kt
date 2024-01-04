package no.nav.etterlatte.brev.dokarkiv

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.JournalpostKoder.Companion.BREV_KODE
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import org.slf4j.LoggerFactory
import java.util.Base64

interface DokarkivService {
    suspend fun journalfoer(
        brevId: BrevID,
        vedtak: VedtakTilJournalfoering,
    ): OpprettJournalpostResponse

    suspend fun journalfoer(request: JournalfoeringsMappingRequest): OpprettJournalpostResponse

    suspend fun oppdater(
        journalpostId: String,
        forsoekFerdistill: Boolean,
        journalfoerendeEnhet: String?,
        request: OppdaterJournalpostRequest,
    ): OppdaterJournalpostResponse

    suspend fun ferdigstill(
        journalpostId: String,
        sak: Sak,
    )

    suspend fun endreTema(
        journalpostId: String,
        nyttTema: String,
    )

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
    ): OpprettJournalpostResponse = journalfoer(brevId) { mapTilJournalpostRequest(brevId, vedtak) }

    override suspend fun journalfoer(request: JournalfoeringsMappingRequest): OpprettJournalpostResponse {
        return journalfoer(request.brevId) { mapTilJournalpostRequest(request) }
    }

    private suspend fun journalfoer(
        brevId: BrevID,
        request: () -> OpprettJournalpostRequest,
    ): OpprettJournalpostResponse {
        logger.info("Oppretter journalpost for brev med id=$brevId")
        return client.opprettJournalpost(request(), true).also {
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

    override suspend fun ferdigstill(
        journalpostId: String,
        sak: Sak,
    ) {
        val request =
            OppdaterJournalpostSakRequest(
                tema = sak.sakType.tema,
                bruker = Bruker(id = sak.ident),
                sak =
                    JournalpostSak(
                        sakstype = Sakstype.FAGSAK,
                        fagsakId = sak.id.toString(),
                        tema = sak.sakType.tema,
                    ),
            )

        client.oppdaterFagsak(journalpostId, request)
        client.ferdigstillJournalpost(journalpostId, sak.enhet)

        logger.info("Journalpost med id=$journalpostId ferdigstilt")
    }

    override suspend fun endreTema(
        journalpostId: String,
        nyttTema: String,
    ) {
        client.endreTema(journalpostId, nyttTema)
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
    ): OpprettJournalpostRequest =
        mapTilJournalpostRequest(
            JournalfoeringsMappingRequest(
                brevId = brevId,
                brev = requireNotNull(db.hentBrev(brevId)),
                brukerident = vedtak.sak.ident,
                eksternReferansePrefiks = vedtak.behandlingId,
                sakId = vedtak.sak.id,
                sakType = vedtak.sak.sakType,
                journalfoerendeEnhet = vedtak.ansvarligEnhet,
            ),
        )

    private fun mapTilJournalpostRequest(request: JournalfoeringsMappingRequest): OpprettJournalpostRequest {
        val innhold = requireNotNull(db.hentBrevInnhold(request.brevId))
        val pdf = requireNotNull(db.hentPdf(request.brevId))
        return OpprettJournalpostRequest(
            tittel = innhold.tittel,
            journalposttype = JournalPostType.UTGAAENDE,
            avsenderMottaker = request.avsenderMottaker(),
            bruker = Bruker(request.brukerident),
            eksternReferanseId = "${request.eksternReferansePrefiks}.${request.brevId}",
            sak = JournalpostSak(Sakstype.FAGSAK, request.sakId.toString()),
            dokumenter = listOf(pdf.tilJournalpostDokument(innhold.tittel)),
            tema = request.sakType.tema,
            kanal = "S",
            journalfoerendeEnhet = request.journalfoerendeEnhet,
        )
    }

    private fun Pdf.tilJournalpostDokument(tittel: String) =
        JournalpostDokument(
            tittel,
            brevkode = BREV_KODE,
            dokumentvarianter = listOf(DokumentVariant.ArkivPDF(Base64.getEncoder().encodeToString(bytes))),
        )
}

data class JournalfoeringsMappingRequest(
    val brevId: BrevID,
    val brev: Brev,
    val brukerident: String,
    val eksternReferansePrefiks: Any,
    val sakId: Long,
    val sakType: SakType,
    val journalfoerendeEnhet: String,
) {
    fun avsenderMottaker() = brev.avsenderMottaker()

    private fun Brev.avsenderMottaker(): AvsenderMottaker =
        AvsenderMottaker(
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
