package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType.Application.Json
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson


interface Pdl {
    suspend fun hentPerson(variables: PdlVariables): PdlPersonResponse
}

class PdlKlient(val httpClient: HttpClient) : Pdl {

    companion object {
        const val TEMA = "PEN"
    }

    override suspend fun hentPerson(variables: PdlVariables): PdlPersonResponse {
        val request = PdlGraphqlRequest(
            query = getQuery("/pdl/hentPerson.graphql"),
            variables = variables
        )

        return retry<PdlPersonResponse> {
            httpClient.post {
                header("Tema", TEMA)
                accept(Json)
                body = TextContent(request.toJson(), Json)
            }
        }.let{
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.exceptions.last()
            }
        }
    }

    private fun getQuery(name: String): String {
        return javaClass.getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")
    }
}
