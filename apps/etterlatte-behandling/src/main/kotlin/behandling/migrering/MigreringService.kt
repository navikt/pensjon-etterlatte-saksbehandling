package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class MigreringService(
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun avbrytBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val status = behandlingService.hentBehandling(behandlingId)!!.status
        if (!status.kanAvbrytes()) {
            logger.warn("Behandling $behandlingId kan ikke avbrytes, fordi den har status $status.")
            return
        }
        behandlingService.avbrytBehandling(behandlingId, brukerTokenInfo)
    }
}
