package no.nav.etterlatte.hendelserpdl.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.retry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PdlKlient(
    private val httpClient: HttpClient,
    private val url: String
) {
    suspend fun hentPdlIdentifikator(fnr: String): PdlIdentifikator {
        logger.info("Henter ident fra PDL for fnr=${fnr.maskerFnr()}")

        return retry<PdlIdentifikator> {
            httpClient.post("$url/pdlident") {
                contentType(ContentType.Application.Json)
                setBody(HentPdlIdentRequest(PersonIdent(fnr)))
            }.body()
        }.let { result ->
            when (result) {
                is RetryResult.Success -> result.content
                is RetryResult.Failure -> {
                    logger.error("Feil ved henting av ident fra PDL for fnr=${fnr.maskerFnr()}")
                    throw result.samlaExceptions()
                }
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlKlient::class.java)
    }
}