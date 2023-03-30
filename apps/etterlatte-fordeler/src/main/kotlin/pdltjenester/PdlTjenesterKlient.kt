package no.nav.etterlatte.pdltjenester

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.retry
import org.slf4j.LoggerFactory

class PdlTjenesterKlient(private val client: HttpClient, private val apiUrl: String) {

    private val logger = LoggerFactory.getLogger(PdlTjenesterKlient::class.java)

    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person {
        logger.info("Henter person med ${hentPersonRequest.foedselsnummer} fra pdltjenester")
        return retry<Person> {
            client.post(apiUrl) {
                accept(Json)
                contentType(Json)
                setBody(hentPersonRequest)
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    val exception = it.exceptions.last()
                    if (exception is ClientRequestException && exception.response.status == HttpStatusCode.NotFound) {
                        throw PersonFinnesIkkeException(hentPersonRequest.foedselsnummer)
                    }
                    throw it.exceptions.last()
                }
            }
        }
    }
}

data class PersonFinnesIkkeException(val fnr: Folkeregisteridentifikator) : Exception()