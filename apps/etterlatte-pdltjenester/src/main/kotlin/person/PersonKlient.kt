package no.nav.etterlatte.person

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.content.*
import io.ktor.http.ContentType.Application.Json
import no.nav.etterlatte.common.mapJsonToAny
import no.nav.etterlatte.common.toJson
import no.nav.etterlatte.common.unsafeRetry
import no.nav.etterlatte.person.pdl.GraphqlRequest
import no.nav.etterlatte.person.pdl.PdlVariables
import no.nav.etterlatte.person.pdl.PersonResponse
import org.slf4j.LoggerFactory


interface Pdl {
    suspend fun hentPerson(variables: PdlVariables): PersonResponse

}

class PersonKlient(val httpClient: HttpClient) : Pdl {
    val logger = LoggerFactory.getLogger(PersonKlient::class.java)

    companion object {
        const val TEMA = "PEN"
    }

    override suspend fun hentPerson(variables: PdlVariables): PersonResponse {
        val query = getQuery("/pdl/hentUtvidetPerson.graphql")
        val request = GraphqlRequest(query, variables).toJson()
        return safeCall(request)
    }


    suspend inline fun <reified T> safeCall(request: String): T {
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
