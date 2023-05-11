package no.nav.etterlatte.common.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PdlFeil
import no.nav.etterlatte.libs.common.pdl.PdlFeilAarsak
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.FamilieRelasjonManglerIdent
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.retry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface PdlKlient {
    fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle, saktype: SakType): PersonDTO
    fun hentGeografiskTilknytning(foedselsnummer: String, saktype: SakType): GeografiskTilknytning
    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person
}

class PdlKlientImpl(
    private val client: HttpClient,
    private val url: String
) : PdlKlient {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlKlientImpl::class.java)
    }

    override fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle, saktype: SakType): PersonDTO {
        logger.info("Henter Pdl-modell for ${rolle.name}")
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, saktype)
        val response = runBlocking {
            client.post("$url/person/v2") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<PersonDTO>()
        }
        return response
    }

    override fun hentGeografiskTilknytning(foedselsnummer: String, saktype: SakType): GeografiskTilknytning {
        val request = HentGeografiskTilknytningRequest(Folkeregisteridentifikator.of(foedselsnummer), saktype)
        val response = runBlocking {
            client.post("$url/geografisktilknytning") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<GeografiskTilknytning>()
        }

        return response
    }

    override suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person {
        logger.info("Henter person med ${hentPersonRequest.foedselsnummer} fra pdltjenester")
        return retry<Person> {
            client.post(url) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
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

fun PersonDTO.hentDoedsdato(): LocalDate? = this.doedsdato?.verdi

fun PersonDTO.hentAnsvarligeForeldre(): List<Folkeregisteridentifikator>? =
    this.familieRelasjon?.verdi?.ansvarligeForeldre

fun PersonDTO.hentBarn(): List<Folkeregisteridentifikator>? = this.familieRelasjon?.verdi?.barn

fun PersonDTO.hentVergemaal(): List<VergemaalEllerFremtidsfullmakt>? = this.vergemaalEllerFremtidsfullmakt?.map {
    it.verdi
}

fun PersonDTO.hentUtland(): Utland? = this.utland?.verdi