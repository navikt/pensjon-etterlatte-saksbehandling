package no.nav.etterlatte.common.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdenterForAktoerIdBolkRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface PdlTjenesterKlient {
    fun hentPdlModell(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDTO

    fun hentPdlModell(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktyper: List<SakType>,
    ): PersonDTO

    fun hentGeografiskTilknytning(
        foedselsnummer: String,
        saktype: SakType,
    ): GeografiskTilknytning

    fun hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds: Set<String>): Map<String, String?>

    suspend fun hentAdressebeskyttelseForPerson(hentAdressebeskyttelseRequest: HentAdressebeskyttelseRequest): AdressebeskyttelseGradering
}

class PdlTjenesterKlientImpl(config: Config, private val pdl_app: HttpClient) : PdlTjenesterKlient {
    private val url = config.getString("pdltjenester.url")

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PdlTjenesterKlientImpl::class.java)
    }

    override suspend fun hentAdressebeskyttelseForPerson(
        hentAdressebeskyttelseRequest: HentAdressebeskyttelseRequest,
    ): AdressebeskyttelseGradering {
        logger.info("Henter person med ${hentAdressebeskyttelseRequest.ident.value.maskerFnr()} fra pdltjenester")
        return pdl_app.post("$url/person/adressebeskyttelse") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(hentAdressebeskyttelseRequest)
        }.body<AdressebeskyttelseGradering>()
    }

    override fun hentPdlModell(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktyper: List<SakType>,
    ): PersonDTO {
        logger.info("Henter Pdl-modell for rolle ${rolle.name}")
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, saktyper)
        val response =
            runBlocking {
                pdl_app.post("$url/person/v2") {
                    contentType(ContentType.Application.Json)
                    setBody(personRequest)
                }.body<PersonDTO>()
            }
        return response
    }

    override fun hentPdlModell(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDTO {
        logger.info("Henter Pdl-modell for rolle ${rolle.name}")
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, listOf(saktype))
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
