package no.nav.etterlatte.behandling

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import org.slf4j.LoggerFactory


class BehandlingKlient(private val httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    suspend fun hentPerson(fnr: String): String {
        try {
            httpClient.get("/personer/{fnr}/saker") {
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            logger.error("Henting av person fra behandling feilet", e)
        }

        return "tull og tøys"
    }
}
