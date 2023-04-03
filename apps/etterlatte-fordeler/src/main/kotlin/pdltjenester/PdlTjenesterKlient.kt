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
import no.nav.etterlatte.libs.common.pdl.PdlFeil
import no.nav.etterlatte.libs.common.pdl.PdlFeilAarsak
import no.nav.etterlatte.libs.common.person.FamilieRelasjonManglerIdent
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
                    val response = when (exception) {
                        is ClientRequestException -> exception.response
                        is ServerResponseException -> exception.response
                        else -> throw exception
                    }
                    val feilFraPdl = try {
                        response.body<PdlFeil>()
                    } catch (e: Exception) {
                        throw exception
                    }
                    when (feilFraPdl.aarsak) {
                        PdlFeilAarsak.FANT_IKKE_PERSON ->
                            throw PersonFinnesIkkeException(hentPersonRequest.foedselsnummer)

                        PdlFeilAarsak.INGEN_IDENT_FAMILIERELASJON -> throw FamilieRelasjonManglerIdent(
                            "${hentPersonRequest.foedselsnummer} har en person i persongalleriet som " +
                                "mangler ident: ${feilFraPdl.detaljer}"
                        )
                    }
                }
            }
        }
    }
}

data class PersonFinnesIkkeException(val fnr: Folkeregisteridentifikator) : Exception()