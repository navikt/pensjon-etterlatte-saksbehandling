package no.nav.etterlatte.brev.dokarkiv

import no.nav.etterlatte.common.Enhet
import org.slf4j.LoggerFactory

interface DokarkivService {
    suspend fun journalfoer(request: OpprettJournalpost): OpprettJournalpostResponse

    suspend fun oppdater(
        journalpostId: String,
        forsoekFerdigstill: Boolean,
        journalfoerendeEnhet: Enhet?,
        request: OppdaterJournalpostRequest,
    ): OppdaterJournalpostResponse

    suspend fun ferdigstillJournalpost(
        journalpostId: String,
        enhet: Enhet,
    ): Boolean

    suspend fun feilregistrerSakstilknytning(journalpostId: String)

    suspend fun opphevFeilregistrertSakstilknytning(journalpostId: String)

    suspend fun knyttTilAnnenSak(
        journalpostId: String,
        request: KnyttTilAnnenSakRequest,
    ): KnyttTilAnnenSakResponse
}

internal class DokarkivServiceImpl(
    private val client: DokarkivKlient,
) : DokarkivService {
    private val logger = LoggerFactory.getLogger(DokarkivService::class.java)

    override suspend fun journalfoer(request: OpprettJournalpost): OpprettJournalpostResponse {
        logger.info("Oppretter journalpost med eksternReferanseId: ${request.eksternReferanseId}")

        return client.opprettJournalpost(request, ferdigstill = true)
    }

    override suspend fun oppdater(
        journalpostId: String,
        forsoekFerdigstill: Boolean,
        journalfoerendeEnhet: Enhet?,
        request: OppdaterJournalpostRequest,
    ): OppdaterJournalpostResponse {
        // Hack for å unngå feil mot dokarkiv. Alle generelle saker i dokarkiv får fagsaksystem FS22, men vi kan ikke
        // returnere det samme tilbake ved oppdatering. Må derfor tømme saksobjektet og sette sakstype til GENERELL_SAK
        val response =
            if (request.sak?.fagsaksystem == "FS22") {
                client.oppdaterJournalpost(journalpostId, request.copy(sak = JournalpostSak(Sakstype.GENERELL_SAK)))
            } else {
                client.oppdaterJournalpost(journalpostId, request)
            }

        logger.info("Journalpost med id=$journalpostId oppdatert OK!")

        if (forsoekFerdigstill) {
            if (journalfoerendeEnhet == null) {
                logger.error("Kan ikke ferdigstille journalpost=$journalpostId når enhet mangler")
            } else {
                val ferdigstilt = client.ferdigstillJournalpost(journalpostId, journalfoerendeEnhet)
                logger.info("journalpostId=$journalpostId, ferdigstillrespons='$ferdigstilt'")
            }
        }

        return response
    }

    override suspend fun ferdigstillJournalpost(
        journalpostId: String,
        enhet: Enhet,
    ) = client.ferdigstillJournalpost(journalpostId, enhet)

    override suspend fun feilregistrerSakstilknytning(journalpostId: String) {
        client.feilregistrerSakstilknytning(journalpostId)
    }

    override suspend fun opphevFeilregistrertSakstilknytning(journalpostId: String) {
        client.opphevFeilregistrertSakstilknytning(journalpostId)
    }

    override suspend fun knyttTilAnnenSak(
        journalpostId: String,
        request: KnyttTilAnnenSakRequest,
    ): KnyttTilAnnenSakResponse =
        client.knyttTilAnnenSak(journalpostId, request).also {
            logger.info("Journalpost knyttet til annen sak (nyJournalpostId=${it.nyJournalpostId})\n$request")
        }
}
