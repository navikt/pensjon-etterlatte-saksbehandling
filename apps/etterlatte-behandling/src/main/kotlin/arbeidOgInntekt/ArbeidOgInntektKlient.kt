package no.nav.etterlatte.arbeidOgInntekt

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class ArbeidOgInntektKlient(
    private val client: HttpClient,
    private val url: String,
) {
    private val logger: Logger = LoggerFactory.getLogger(ArbeidOgInntektKlient::class.java)

    private val cache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build<String, String>()

    suspend fun hentURLForInntektOversikt(fnr: String): String? {
        logger.info("Henter url for inntekt oversikt")

        val urlForInntektOversikt = cache.getIfPresent(fnr)

        if (urlForInntektOversikt != null) return urlForInntektOversikt

        return try {
            val response =
                client.get("$url/api/v2/redirect/sok/a-inntekt") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    headers {
                        set("Nav-Personident", fnr)
                    }
                }

            if (response.status.isSuccess()) {
                response.body<String>().also {
                    logger.info("Hentet URL fra Aktivitet og inntekt")
                    cache.put(fnr, it)
                }
            } else {
                throw ClientRequestException(response, response.toString())
            }
        } catch (cause: Throwable) {
            logger.warn("Klarte ikke å hente URL fra Aktivitet og inntekt.", cause)
            return null
        }
    }
}
