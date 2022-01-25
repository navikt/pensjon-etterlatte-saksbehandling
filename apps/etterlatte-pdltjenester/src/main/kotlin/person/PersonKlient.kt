package no.nav.etterlatte.person

import com.fasterxml.jackson.databind.JsonNode
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
import person.pdl.UtlandResponse

interface Pdl {
    suspend fun hentPerson(fnr: Foedselsnummer): PersonResponse
    suspend fun hentUtland(fnr: Foedselsnummer): UtlandResponse
}

class PersonKlient(val httpClient: HttpClient) : Pdl {
    val logger = LoggerFactory.getLogger(PersonKlient::class.java)

    companion object {
        const val TEMA = "PEN"
    }

    override suspend fun hentPerson(fnr: Foedselsnummer): PersonResponse {
        val query = getQuery("/pdl/hentPerson.graphql")
        val request = GraphqlRequest(query, Variables(ident = fnr.value)).toJson()
        return PersonResponse(safeCall(request))
    }

    override suspend fun hentUtland(fnr: Foedselsnummer): UtlandResponse {
        val query = getQuery("/pdl/hentUtland.graphql")
        val request = GraphqlRequest(query, Variables(ident = fnr.value, historikk = false)).toJson()
        return UtlandResponse(safeCall(request))
    }

    private suspend inline fun <reified T> safeCall(request: String): T {
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
            logger.error("Error under deserialisering av respons", e)
            println("Error under deserialisering av respons $e")
            throw e
        }
    }

    private fun getQuery(name: String): String {
        return javaClass.getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")
    }
}
