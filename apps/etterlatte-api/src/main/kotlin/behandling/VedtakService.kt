package no.nav.etterlatte.behandling

import org.slf4j.LoggerFactory

data class AttesteringResult(val response: String)

class VedtakService(private val behandlingKlient: BehandlingKlient, private val vedtakKlient: VedtakKlient) {

    private val logger = LoggerFactory.getLogger(VedtakService::class.java)

    suspend fun fattVedtak(behandlingId: String, token: String): AttesteringResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        vedtakKlient.fattVedtak(behandling.sak.toInt(), behandling.id.toString(), token)
        return AttesteringResult("Fattet")
    }

    suspend fun attesterVedtak(behandlingId: String, token: String): AttesteringResult {
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        vedtakKlient.attesterVedtak(behandling.sak.toInt(), behandling.id.toString(), token)
        return AttesteringResult("Attestert")
    }

    suspend fun underkjennVedtak(behandlingId: String, begrunnelse: String, kommentar: String, token: String): AttesteringResult {
        logger.info("UnderkjennVetak i servicen etterlatte-api med id: ", behandlingId )
        logger.info("UnderkjennVetak i servicen etterlatte-api begrunnelse: ", begrunnelse)
        logger.info("UnderkjennVetak i servicen etterlatte-api kommentar ", kommentar)
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        vedtakKlient.underkjennVedtak(behandling.sak.toInt(), behandling.id.toString(), begrunnelse, kommentar, token)
        return AttesteringResult("Underkjent")
    }

}