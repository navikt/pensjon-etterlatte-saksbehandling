package no.nav.etterlatte.brev.notat

import no.nav.etterlatte.brev.BehandlingsvurderingNotatRequest
import no.nav.etterlatte.brev.NyNotatService
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class BehandlingsvurderingNotatService(
    private val nyNotatService: NyNotatService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettOgJournalfoer(
        behandlingId: UUID,
        request: BehandlingsvurderingNotatRequest,
        bruker: BrukerTokenInfo,
    ) {
        if (nyNotatService.hentForReferanse(behandlingId.toString()).isNotEmpty()) {
            throw BehandlingsvurderingAlleredeJournalfoertException(behandlingId)
        }

        logger.info("Journalfører behandlingsvurdering for behandling $behandlingId")

        nyNotatService.opprettOgJournalfoerMedPayload(
            sakId = request.sakId,
            mal = NotatMal.BEHANDLINGSVURDERING,
            tittel = "Behandlingsvurdering",
            referanse = behandlingId.toString(),
            payload = request.slate,
            bruker = bruker,
        )
    }
}

class BehandlingsvurderingAlleredeJournalfoertException(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BEHANDLINGSVURDERING_ALLEREDE_JOURNALFOERT",
        detail = "Behandlingsvurdering er allerede journalført for behandling $behandlingId",
    )
