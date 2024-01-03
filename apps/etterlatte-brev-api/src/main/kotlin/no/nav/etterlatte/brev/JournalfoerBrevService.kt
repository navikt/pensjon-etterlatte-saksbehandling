package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.hentinformasjon.SakService
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class JournalfoerBrevService(
    private val db: BrevRepository,
    private val sakService: SakService,
    private val dokarkivService: DokarkivService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun journalfoer(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): String {
        val brev = db.hentBrev(id)

        if (brev.status != Status.FERDIGSTILT) {
            throw IllegalStateException("Ugyldig status ${brev.status} på brev (id=${brev.id})")
        }

        val sak = sakService.hentSak(brev.sakId, bruker)

        val response = dokarkivService.journalfoer(brev, sak)

        db.settBrevJournalfoert(brev.id, response)
        logger.info("Brev med id=${brev.id} markert som journalført")

        return response.journalpostId
    }
}
