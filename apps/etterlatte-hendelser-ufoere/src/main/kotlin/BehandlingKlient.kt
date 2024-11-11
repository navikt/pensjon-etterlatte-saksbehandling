package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.hendelserufoere.UfoeretrygdHendelse
import org.slf4j.LoggerFactory

class BehandlingKlient(
    val behandlingHttpClient: HttpClient,
    val resourceUrl: String,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    suspend fun postTilBehandling(ufoereHendelse: UfoeretrygdHendelse) {
        logger.info("Lagrer hendelse om uføretrygd i behandling")

        behandlingHttpClient.post(
            "$resourceUrl/grunnlagsendringshendelse/ufoeretrygd",
        ) {
            contentType(ContentType.Application.Json)
            setBody(ufoereHendelse)
        }
    }
}
