package no.nav.etterlatte.behandling

import org.slf4j.LoggerFactory

data class AttesteringResult(val response: String)

class VedtakService(private val behandlingKlient: BehandlingKlient) {

    private val logger = LoggerFactory.getLogger(VedtakService::class.java)

    suspend fun sendTilAttestering(behandlingId: String, token: String): AttesteringResult {

        behandlingKlient.sendTilAttestering(behandlingId, token)
        return AttesteringResult("Dette gikk nok ikke")
    }

}