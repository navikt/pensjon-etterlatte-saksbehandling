package no.nav.etterlatte.common.klienter

import com.typesafe.config.Config
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
import no.nav.etterlatte.libs.common.feilhaandtering.ExceptionResponse
import no.nav.etterlatte.libs.common.logging.samleExceptions
import no.nav.etterlatte.libs.common.pdl.PdlFeilAarsak
import no.nav.etterlatte.libs.common.pdl.PdlInternalServerError
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdenterForAktoerIdBolkRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.retry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface PdlKlient {
    fun hentPdlModell(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDTO

    fun hentGeografiskTilknytning(
        foedselsnummer: String,
        saktype: SakType,
    ): GeografiskTilknytning

    fun hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds: Set<String>): Map<String, String?>

    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person?
}

class PdlKlientImpl(config: Config, private val pdl_app: HttpClient) : PdlKlient {
    private val url = config.getString("pdltjenester.url")

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlKlientImpl::class.java)
    }

    override suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person? {
        logger.info("Henter person med ${hentPersonRequest.foedselsnummer.value.maskerFnr()} fra pdltjenester")
        return retry<Person> {
            pdl_app.post("$url/person") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
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

    override fun hentPdlModell(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDTO {
        logger.info("Henter Pdl-modell for rolle ${rolle.name}")
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, saktype)
        val response =
            runBlocking {
                pdl_app.post("$url/person/v2") {
                    contentType(ContentType.Application.Json)
                    setBody(personRequest)
                }.body<PersonDTO>()
            }
        return response
    }

    override fun hentGeografiskTilknytning(
        foedselsnummer: String,
        saktype: SakType,
    ): GeografiskTilknytning {
        val request = HentGeografiskTilknytningRequest(Folkeregisteridentifikator.of(foedselsnummer), saktype)
        val response =
            runBlocking {
                pdl_app.post("$url/geografisktilknytning") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<GeografiskTilknytning>()
            }

        return response
    }

    override fun hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds: Set<String>): Map<String, String?> {
        val request = HentFolkeregisterIdenterForAktoerIdBolkRequest(aktoerIds)
        val response =
            runBlocking {
                pdl_app.post("$url/folkeregisteridenter") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<Map<String, String?>>()
            }
        return response
    }
}

fun PersonDTO.hentDoedsdato(): LocalDate? = this.doedsdato?.verdi

fun PersonDTO.hentAnsvarligeForeldre(): List<Folkeregisteridentifikator>? = this.familieRelasjon?.verdi?.ansvarligeForeldre

fun PersonDTO.hentBarn(): List<Folkeregisteridentifikator>? = this.familieRelasjon?.verdi?.barn

fun PersonDTO.hentVergemaal(): List<VergemaalEllerFremtidsfullmakt>? =
    this.vergemaalEllerFremtidsfullmakt?.map {
        it.verdi
    }

fun PersonDTO.hentSivilstand(): List<Sivilstand>? =
    this.sivilstand?.map {
        it.verdi
    }

fun PersonDTO.hentBostedsadresse(): List<Adresse>? =
    this.bostedsadresse?.map {
        it.verdi
    }

fun PersonDTO.hentUtland(): Utland? = this.utland?.verdi
