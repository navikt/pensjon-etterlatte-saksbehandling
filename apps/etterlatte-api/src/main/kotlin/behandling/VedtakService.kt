package no.nav.etterlatte.behandling

import org.slf4j.LoggerFactory

data class VedtakResult(val response: String)

class VedtakService(private val behandlingKlient: BehandlingKlient) {

    private val logger = LoggerFactory.getLogger(VedtakService::class.java)

    suspend fun fattVedtak(behandlingId: String, token: String): VedtakResult {

        behandlingKlient.sendTilAttestering(behandlingId, token)
        return VedtakResult("Dette gikk nok ikke")
    }

}