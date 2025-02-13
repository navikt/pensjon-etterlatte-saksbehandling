package no.nav.etterlatte.person.krr

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

interface KrrKlient {
    suspend fun hentDigitalKontaktinformasjon(fnr: String): DigitalKontaktinformasjon?
}

class KrrKlientImpl(
    private val client: HttpClient,
    private val url: String,
) : KrrKlient {
    private val logger: Logger = LoggerFactory.getLogger(KrrKlientImpl::class.java)

    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build<String, DigitalKontaktinformasjon>()

    override suspend fun hentDigitalKontaktinformasjon(fnr: String): DigitalKontaktinformasjon? {
        logger.info("Henter kontaktopplysninger fra KRR.")

        val kontaktinformasjon = cache.getIfPresent(fnr)

        if (kontaktinformasjon != null) {
            return kontaktinformasjon
        }

        return try {
            val response =
                client.get("$url/rest/v1/person") {
                    header("Nav-Personident", fnr)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }

            if (response.status.isSuccess()) {
                response
                    .body<DigitalKontaktinformasjon?>()
                    ?.also {
                        logger.info("Hentet kontaktinformasjon fra KRR")
                        cache.put(fnr, it)
                    }
            } else {
                throw ClientRequestException(response, response.toString())
            }
        } catch (cause: Throwable) {
            logger.warn("Klarte ikke å hente kontaktinformasjon fra KRR.", cause)
            return null
        }
    }
}
