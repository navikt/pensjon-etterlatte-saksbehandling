package no.nav.etterlatte.pdltjenester

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.contentType
import no.nav.etterlatte.common.RetryResult
import no.nav.etterlatte.common.retry
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import org.slf4j.LoggerFactory

class PdlTjenesterKlient(private val client: HttpClient, private val apiUrl: String) {

    private val logger = LoggerFactory.getLogger(PdlTjenesterKlient::class.java)

    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person {
        logger.info("Henter person med ${hentPersonRequest.foedselsnummer} fra pdltjenester")
        return retry<Person>{
            client.post(apiUrl){
                accept(Json)
                contentType(Json)
                body = hentPersonRequest
            }
        }.let{
            when (it) {
                is RetryResult.Success ->it.content
                is RetryResult.Failure -> throw it.exceptions.last()
            }
        }
    }
}
