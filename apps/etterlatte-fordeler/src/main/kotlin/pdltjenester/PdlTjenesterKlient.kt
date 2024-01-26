package no.nav.etterlatte.pdltjenester

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.feilhaandtering.ExceptionResponse
import no.nav.etterlatte.libs.common.logging.samleExceptions
import no.nav.etterlatte.libs.common.pdl.PdlFeilAarsak
import no.nav.etterlatte.libs.common.pdl.PdlInternalServerError
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.retry
import org.slf4j.LoggerFactory

class PdlTjenesterKlient(private val client: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(PdlTjenesterKlient::class.java)

    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person? {
        logger.info("Henter person med ${hentPersonRequest.foedselsnummer.value.maskerFnr()} fra pdltjenester")
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
                    val response =
                        when (val exception = it.exceptions.last()) {
                            is ClientRequestException -> exception.response
                            is ServerResponseException -> exception.response
                            else -> throw it.samlaExceptions()
                        }
                    val feilFraPdl =
                        try {
                            val feil = response.body<ExceptionResponse>()
                            enumValueOf<PdlFeilAarsak>(feil.code!!)
                        } catch (e: Exception) {
                            throw samleExceptions(it.exceptions + e)
                        }
                    when (feilFraPdl) {
                        PdlFeilAarsak.FANT_IKKE_PERSON -> null
                        PdlFeilAarsak.INTERNAL_SERVER_ERROR -> throw PdlInternalServerError()
                    }
                }
            }
        }
    }
}

data class PersonFinnesIkkeException(val fnr: Folkeregisteridentifikator) : Exception()
