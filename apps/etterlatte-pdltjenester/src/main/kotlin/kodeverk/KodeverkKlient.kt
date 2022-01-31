package no.nav.etterlatte.kodeverk

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import org.slf4j.LoggerFactory

interface Kodeverk {
    suspend fun hentPostnummer(): KodeverkResponse
    suspend fun hentLandkoder(): KodeverkResponse
}

/**
 * Klient som kaller på kodeverksklienten via
 */
class KodeverkKlient(private val httpClient: HttpClient) : Kodeverk {
    private val logger = LoggerFactory.getLogger(KodeverkService::class.java)

    override suspend fun hentPostnummer(): KodeverkResponse =
        try {
            logger.info("Henter alle postnummer fra Kodeverk")

            httpClient.get("Postnummer?ekskluderUgyldige=true") {
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            logger.error("Henting av postnummere feilet", e)
            throw e
        }

    override suspend fun hentLandkoder(): KodeverkResponse =
        try {
            logger.info("Henter alle landkoder fra Kodeverk")

            httpClient.get("Landkoder?ekskluderUgyldige=false") {
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            logger.error("Henting av landkoder feilet", e)
            throw e
        }
}
