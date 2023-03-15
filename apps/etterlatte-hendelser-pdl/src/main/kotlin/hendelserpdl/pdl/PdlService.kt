package no.nav.etterlatte.hendelserpdl.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.FolkeregisterIdent
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Pdl {
    fun hentFolkeregisterIdentifikator(ident: String): FolkeregisterIdent
}

class PdlService(
    private val pdl_app: HttpClient,
    private val url: String
) : Pdl {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlService::class.java)
    }

    override fun hentFolkeregisterIdentifikator(ident: String): FolkeregisterIdent {
        logger.info("Henter folkeregisteridentifikator")
        return runBlocking {
            try {
                pdl_app.post("$url/folkeregisterident") {
                    contentType(ContentType.Application.Json)
                    setBody(HentFolkeregisterIdentRequest(PersonIdent(ident)))
                }.body()
            } catch (e: Exception) {
                logger.info("Kunne ikke hente folkeregisteridentifikator", e)
                throw e
            }
        }
    }
}