package no.nav.etterlatte.tilbakekreving.klienter

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import org.slf4j.LoggerFactory

class BehandlingKlient(
    private val url: String,
    private val httpClient: HttpClient
) {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    suspend fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag) {
        logger.info("Oppretter tilbakekreving i behandling")
        try {
            httpClient.post("$url/tilbakekreving") {
                contentType(ContentType.Application.Json)
                setBody(kravgrunnlag)
            }
        } catch (e: Exception) {
            throw Exception("Klarte ikke å opprette behandling for tilbakekreving=${kravgrunnlag.kravgrunnlagId.value}")
        }
    }
}