package no.nav.etterlatte.behandling

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import org.slf4j.LoggerFactory

interface EtterlatteBehandling {
    suspend fun hentPerson(fnr: String): List<Sak>
}

class BehandlingKlient(private val httpClient: HttpClient): EtterlatteBehandling {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    override suspend fun hentPerson(fnr: String): List<Sak> =
        try {
            logger.info("Henter saker fra behandling")
            httpClient.get("/personer/{fnr}/saker") {
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            logger.error("Henting av person fra behandling feilet", e)
            throw e
        }
}

