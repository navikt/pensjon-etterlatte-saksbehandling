package no.nav.etterlatte.migrering.person.krr

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Krr {
    suspend fun hentDigitalKontaktinformasjon(fnr: Folkeregisteridentifikator): DigitalKontaktinformasjon?
}

class KrrKlient(private val client: HttpClient, config: Config) : Krr {
    private val logger: Logger = LoggerFactory.getLogger(KrrKlient::class.java)
    private val url = config.getString("krr.url")

    override suspend fun hentDigitalKontaktinformasjon(fnr: Folkeregisteridentifikator): DigitalKontaktinformasjon? {
        logger.info("Henter kontaktopplysninger fra KRR.")

        return try {
            val response =
                client.get("$url/person") {
                    header(HttpHeaders.NavPersonIdent, fnr.value)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                response.body()
            } else {
                throw ClientRequestException(response, response.toString())
            }
        } catch (cause: Throwable) {
            logger.warn("Klarte ikke å hente kontaktinformasjon fra KRR.", KrrException(cause))
            return null
        }
    }
}

class KrrException(cause: Throwable) :
    Exception("Klarte ikke å hente digital kontaktinfo fra Krr", cause)

val HttpHeaders.NavPersonIdent: String
    get() = "Nav-Personident"
