package no.nav.etterlatte.behandling


data class AttesteringResult(val response: String)

class VedtakService(private val behandlingKlient: BehandlingKlient, private val vedtakKlient: VedtakKlient) {

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
        val behandling = behandlingKlient.hentBehandling(behandlingId, token)
        vedtakKlient.underkjennVedtak(behandling.sak.toInt(), behandling.id.toString(), begrunnelse, kommentar, token)
        return AttesteringResult("Underkjent")
    }

}
