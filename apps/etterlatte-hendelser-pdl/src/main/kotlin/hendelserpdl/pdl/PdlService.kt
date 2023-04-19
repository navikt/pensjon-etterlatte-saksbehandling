package no.nav.etterlatte.hendelserpdl.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.PersonIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Pdl {
    suspend fun hentPdlIdentifikator(fnr: String): PdlIdentifikator
}

class PdlService(
    private val pdl_app: HttpClient,
    private val url: String
) : Pdl {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlService::class.java)
    }

    override suspend fun hentPdlIdentifikator(fnr: String): PdlIdentifikator {
        logger.info("Henter folkeregisteridentifikator")
        try {
            return pdl_app.post("$url/pdlident") {
                contentType(ContentType.Application.Json)
                // TODO: uavklart om vi må slå opp i behandlingsdatabasen for å finne saktypen for å finne behandlingsnummer
                // kan potensielt føre til at vi ikke finner personen. Men siden dette er en testfase kan vi vente med en avklaring
                setBody(HentPdlIdentRequest(PersonIdent(fnr), SakType.BARNEPENSJON))
            }.body()
        } catch (e: Exception) {
            logger.info("Kunne ikke hente pdlident", e)
            throw e
        }
    }
}