package no.nav.etterlatte.statistikk.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VedtaksvurderingsKlient(
    private val httpClient: HttpClient,
    private val vedtakUrl: String,
) {
    private val logger: Logger = LoggerFactory.getLogger(VedtaksvurderingsKlient::class.java)

    suspend fun hentVedtak(behandlingId: String): VedtakDto? =
        try {
            httpClient.get("$vedtakUrl/api/vedtak/$behandlingId").body()
        } catch (e: Exception) {
            logger.warn("Kunne ikke hente vedtaket for behandlingen med id=$behandlingId", e)
            null
        }
}
