package no.nav.etterlatte.migrering.person.krr

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KrrKlient(private val client: HttpClient, private val url: String) {
    private val logger: Logger = LoggerFactory.getLogger(KrrKlient::class.java)

    suspend fun hentDigitalKontaktinformasjon(fnr: Folkeregisteridentifikator): DigitalKontaktinformasjon? {
        logger.info("Henter kontaktopplysninger fra KRR.")

        return try {
            val response =
                client.get("$url/rest/v1/person") {
                    header("Nav-Personident", fnr.value)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                response.body<DigitalKontaktinformasjon?>()
                    .also { logger.info("Hentet kontaktinformasjon fra KRR. Var null? ${it != null}") }
            } else {
                throw ClientRequestException(response, response.toString())
            }
        } catch (cause: Throwable) {
            logger.warn("Klarte ikke å hente kontaktinformasjon fra KRR.", cause)
            return null
        }
    }
}
