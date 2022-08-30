package no.nav.etterlatte.opplysninger.kilde.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

class PdlService(private val pdl: HttpClient, private val url: String) : Pdl {
    override fun hentPerson(foedselsnummer: String, rolle: PersonRolle): Person {
        val personRequest = HentPersonRequest(Foedselsnummer.of(foedselsnummer), rolle)
        val response = runBlocking {
            pdl.post("$url/person") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<Person>()
        }
        return response
    }

    override fun hentOpplysningsperson(foedselsnummer: String, rolle: PersonRolle): PersonDTO {
        val personRequest = HentPersonRequest(Foedselsnummer.of(foedselsnummer), rolle)
        val response = runBlocking {
            pdl.post("$url/person/v2") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<PersonDTO>()
        }
        return response
    }
}

data class PersonDTO(
    val fornavn: OpplysningDTO<String>,
    val etternavn: OpplysningDTO<String>,
    val foedselsnummer: OpplysningDTO<Foedselsnummer>,
    val foedselsdato: OpplysningDTO<LocalDate>?,
    val foedselsaar: OpplysningDTO<Int>,
    val foedeland: OpplysningDTO<String>?,
    val doedsdato: OpplysningDTO<LocalDate>?,
    val adressebeskyttelse: OpplysningDTO<Adressebeskyttelse>?,
    var bostedsadresse: List<OpplysningDTO<Adresse>>?,
    var deltBostedsadresse: List<OpplysningDTO<Adresse>>?,
    var kontaktadresse: List<OpplysningDTO<Adresse>>?,
    var oppholdsadresse: List<OpplysningDTO<Adresse>>?,
    val sivilstatus: OpplysningDTO<Sivilstatus>?,
    val statsborgerskap: OpplysningDTO<String>?,
    var utland: OpplysningDTO<Utland>?,
    var familieRelasjon: OpplysningDTO<FamilieRelasjon>?,
    var avdoedesBarn: List<Person>?,
    var vergemaalEllerFremtidsfullmakt: List<OpplysningDTO<VergemaalEllerFremtidsfullmakt>>?
) {
    fun tilPerson() = Person(
        fornavn = fornavn.verdi,
        etternavn = etternavn.verdi,
        foedselsnummer = foedselsnummer.verdi,
        foedselsdato = foedselsdato?.verdi,
        foedselsaar = foedselsaar.verdi,
        foedeland = foedeland?.verdi,
        doedsdato = doedsdato?.verdi,
        adressebeskyttelse = adressebeskyttelse?.verdi,
        bostedsadresse = bostedsadresse?.map { it.verdi },
        deltBostedsadresse = deltBostedsadresse?.map { it.verdi },
        kontaktadresse = kontaktadresse?.map { it.verdi },
        oppholdsadresse = oppholdsadresse?.map { it.verdi },
        sivilstatus = sivilstatus?.verdi,
        statsborgerskap = statsborgerskap?.verdi,
        utland = utland?.verdi,
        familieRelasjon = familieRelasjon?.verdi,
        avdoedesBarn = avdoedesBarn,
        vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt?.map { it.verdi }
    )
}

data class OpplysningDTO<T>(val verdi: T, val opplysningsid: String?)