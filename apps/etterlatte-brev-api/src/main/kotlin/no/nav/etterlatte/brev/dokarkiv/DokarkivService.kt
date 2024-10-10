package no.nav.etterlatte.brev.dokarkiv

import no.nav.etterlatte.brev.model.OpprettJournalpostResponse
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

interface DokarkivService {
    suspend fun journalfoer(
        request: OpprettJournalpost,
        bruker: BrukerTokenInfo,
    ): OpprettJournalpostResponse

    suspend fun oppdater(
        journalpostId: String,
        forsoekFerdigstill: Boolean,
        journalfoerendeEnhet: Enhetsnummer?,
        request: OppdaterJournalpostRequest,
        bruker: BrukerTokenInfo,
    ): OppdaterJournalpostResponse

    suspend fun ferdigstillJournalpost(
        journalpostId: String,
        enhet: Enhetsnummer,
        bruker: BrukerTokenInfo,
    ): Boolean

    suspend fun feilregistrerSakstilknytning(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    )

    suspend fun settStatusAvbryt(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    )

    suspend fun opphevFeilregistrertSakstilknytning(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    )

    suspend fun knyttTilAnnenSak(
        journalpostId: String,
        request: KnyttTilAnnenSakRequest,
        bruker: BrukerTokenInfo,
    ): KnyttTilAnnenSakResponse
}

internal class DokarkivServiceImpl(
    private val client: DokarkivKlient,
) : DokarkivService {
    private val logger = LoggerFactory.getLogger(DokarkivService::class.java)

    override suspend fun journalfoer(
        request: OpprettJournalpost,
        bruker: BrukerTokenInfo,
    ): OpprettJournalpostResponse {
        logger.info("Oppretter journalpost med eksternReferanseId: ${request.eksternReferanseId}")

        return client.opprettJournalpost(request, ferdigstill = true, bruker)
    }

    override suspend fun oppdater(
        journalpostId: String,
        forsoekFerdigstill: Boolean,
        journalfoerendeEnhet: Enhetsnummer?,
        request: OppdaterJournalpostRequest,
        bruker: BrukerTokenInfo,
    ): OppdaterJournalpostResponse {
        // Hack for å unngå feil mot dokarkiv. Alle generelle saker i dokarkiv får fagsaksystem FS22, men vi kan ikke
        // returnere det samme tilbake ved oppdatering. Må derfor tømme saksobjektet og sette sakstype til GENERELL_SAK
        val response =
            if (request.sak?.fagsaksystem == "FS22") {
                client.oppdaterJournalpost(
                    journalpostId,
                    request.copy(sak = JournalpostSak(Sakstype.GENERELL_SAK)),
                    bruker,
                )
            } else {
                client.oppdaterJournalpost(journalpostId, request, bruker)
            }

        logger.info("Journalpost med id=$journalpostId oppdatert OK!")

        if (forsoekFerdigstill) {
            if (journalfoerendeEnhet == null) {
                logger.error("Kan ikke ferdigstille journalpost=$journalpostId når enhet mangler")
            } else {
                val ferdigstilt = client.ferdigstillJournalpost(journalpostId, journalfoerendeEnhet, bruker)
                logger.info("journalpostId=$journalpostId, ferdigstillrespons='$ferdigstilt'")
            }
        }

        return response
    }

    override suspend fun ferdigstillJournalpost(
        journalpostId: String,
        enhet: Enhetsnummer,
        bruker: BrukerTokenInfo,
    ) = client.ferdigstillJournalpost(journalpostId, enhet, bruker)

    override suspend fun feilregistrerSakstilknytning(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ) {
        client.feilregistrerSakstilknytning(journalpostId, bruker)
    }

    override suspend fun settStatusAvbryt(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ) {
        client.settStatusAvbryt(journalpostId, bruker)
    }

    override suspend fun opphevFeilregistrertSakstilknytning(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ) {
        client.opphevFeilregistrertSakstilknytning(journalpostId, bruker)
    }

    override suspend fun knyttTilAnnenSak(
        journalpostId: String,
        request: KnyttTilAnnenSakRequest,
        bruker: BrukerTokenInfo,
    ): KnyttTilAnnenSakResponse =
        client.knyttTilAnnenSak(journalpostId, request, bruker).also {
            logger.info("Journalpost knyttet til annen sak (nyJournalpostId=${it.nyJournalpostId})\n$request")
        }
}
