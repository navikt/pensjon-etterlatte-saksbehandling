package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.JournalfoeringsMappingRequest
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.hentinformasjon.SakService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class JournalfoerBrevService(
    private val db: BrevRepository,
    private val sakService: SakService,
    private val dokarkivService: DokarkivService,
    private val service: VedtaksbrevService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun journalfoer(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): String {
        val brev = db.hentBrev(id)
        val sak = sakService.hentSak(brev.sakId, bruker)

        return journalfoer(
            brev,
            JournalfoeringsMappingRequest(
                brevId = brev.id,
                brev = brev,
                brukerident = brev.soekerFnr,
                eksternReferansePrefiks = brev.sakId,
                sakId = brev.sakId,
                sakType = sak.sakType,
                journalfoerendeEnhet = sak.enhet,
            ),
        ).journalpostId
    }

    suspend fun journalfoerVedtaksbrev(vedtak: VedtakTilJournalfoering): Pair<OpprettJournalpostResponse, BrevID>? {
        logger.info("Nytt vedtak med id ${vedtak.vedtakId} er attestert. Ferdigstiller vedtaksbrev.")
        val behandlingId = vedtak.behandlingId

        val brev =
            service.hentVedtaksbrev(behandlingId)
                ?: throw NoSuchElementException("Ingen vedtaksbrev funnet på behandlingId=$behandlingId")

        // TODO: Forbedre denne "fiksen". Gjøres nå for å lappe sammen
        if (brev.status in listOf(Status.JOURNALFOERT, Status.DISTRIBUERT, Status.SLETTET)) {
            logger.warn("Vedtaksbrev (id=${brev.id}) er allerede ${brev.status}.")
            return null
        }

        val mappingRequest =
            JournalfoeringsMappingRequest(
                brevId = brev.id,
                brev = requireNotNull(db.hentBrev(brev.id)),
                brukerident = vedtak.sak.ident,
                eksternReferansePrefiks = vedtak.behandlingId,
                sakId = vedtak.sak.id,
                sakType = vedtak.sak.sakType,
                journalfoerendeEnhet = vedtak.ansvarligEnhet,
            )

        return journalfoer(brev, mappingRequest)
            .also { logger.info("Vedtaksbrev for vedtak med id ${vedtak.vedtakId} er journalfoert OK") }
            .let { Pair(it, brev.id) }
    }

    private suspend fun journalfoer(
        brev: Brev,
        mappingRequest: JournalfoeringsMappingRequest,
    ): OpprettJournalpostResponse {
        logger.info("Skal journalføre brev ${brev.id}")
        if (brev.status != Status.FERDIGSTILT) {
            throw FeilStatusForJournalfoering(brev.id, brev.status)
        }

        val response = dokarkivService.journalfoer(mappingRequest)

        if (response.journalpostferdigstilt) {
            db.settBrevJournalfoert(brev.id, response)
        } else {
            logger.info("Kunne ikke ferdigstille journalpost. Forsøker på nytt...")
            dokarkivService.ferdigstillJournalpost(response.journalpostId, mappingRequest.journalfoerendeEnhet)
                .also { db.settBrevJournalfoert(brev.id, response.copy(journalpostferdigstilt = it)) }
        }

        logger.info("Brev med id=${brev.id} markert som journalført")
        return response
    }
}

class FeilStatusForJournalfoering(brevID: BrevID, status: Status) : UgyldigForespoerselException(
    code = "FEIL_STATUS_FOR_JOURNALFOERING",
    detail = "Kan ikke journalføre brev med status ${status.name.lowercase()}",
    meta =
        mapOf(
            "brevId" to brevID,
            "status" to status,
        ),
)
