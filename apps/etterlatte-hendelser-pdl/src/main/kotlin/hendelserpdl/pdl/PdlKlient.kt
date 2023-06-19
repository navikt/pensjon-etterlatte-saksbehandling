package no.nav.etterlatte.hendelserpdl.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PdlKlient(
    private val httpClient: HttpClient,
    private val url: String
) {
    suspend fun hentPdlIdentifikator(fnr: String): PdlIdentifikator {
        logger.info("Henter folkeregisteridentifikator")
        try {
            return httpClient.post("$url/pdlident") {
                contentType(ContentType.Application.Json)
                setBody(HentPdlIdentRequest(PersonIdent(fnr)))
            }.body()
        } catch (e: Exception) {
            logger.info("Kunne ikke hente pdlident for fnr=${fnr.maskerFnr()}", e)
            throw e
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlKlient::class.java)
    }
}