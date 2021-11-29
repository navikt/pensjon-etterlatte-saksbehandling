package no.nav.etterlatte.person

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType.Application.Json
import no.nav.etterlatte.common.mapJsonToAny
import no.nav.etterlatte.common.toJson
import no.nav.etterlatte.common.unsafeRetry
import no.nav.etterlatte.libs.common.pdl.GraphqlRequest
import no.nav.etterlatte.libs.common.pdl.Variables
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.person.pdl.PersonResponse
import org.slf4j.LoggerFactory

interface Pdl {
    suspend fun hentPerson(fnr: Foedselsnummer): PersonResponse
}

class PersonKlient(private val httpClient: HttpClient) : Pdl {
    private val logger = LoggerFactory.getLogger(PersonKlient::class.java)

    companion object {
        private const val TEMA = "PEN"
    }

    override suspend fun hentPerson(fnr: Foedselsnummer): PersonResponse {
        val query = javaClass.getResource("/pdl/hentPerson.graphql")!!
            .readText()
            .replace(Regex("[\n\t]"), "")

        val request = GraphqlRequest(query, Variables(ident = fnr.value)).toJson()

        val responseNode = unsafeRetry {
            httpClient.post<ObjectNode> {
                header("Tema", TEMA)
                accept(Json)
                body = TextContent(request, Json)
            }
        }

        return try {
            mapJsonToAny(responseNode!!.toJson())
        } catch (e: Exception) {
            logger.error("Error under deserialisering av pdl person", e)
            throw e
        }
    }
}
