package no.nav.etterlatte.libs.common.pdl

import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Statsborgerskap
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

data class PersonDoedshendelseDto(
    val foedselsnummer: OpplysningDTO<Folkeregisteridentifikator>,
    val foedselsdato: OpplysningDTO<LocalDate>?,
    val foedselsaar: OpplysningDTO<Int>?,
    val doedsdato: OpplysningDTO<LocalDate>?,
    var bostedsadresse: List<OpplysningDTO<Adresse>>?,
    var deltBostedsadresse: List<OpplysningDTO<Adresse>>?,
    var kontaktadresse: List<OpplysningDTO<Adresse>>?,
    var oppholdsadresse: List<OpplysningDTO<Adresse>>?,
    val sivilstand: List<OpplysningDTO<Sivilstand>>?,
    var utland: OpplysningDTO<Utland>?,
    var familieRelasjon: OpplysningDTO<FamilieRelasjon>?,
    var avdoedesBarn: List<Person>?,
)

data class PersonDTO(
    val fornavn: OpplysningDTO<String>,
    val mellomnavn: OpplysningDTO<String>?,
    val etternavn: OpplysningDTO<String>,
    val foedselsnummer: OpplysningDTO<Folkeregisteridentifikator>,
    val foedselsdato: OpplysningDTO<LocalDate>?,
    val foedselsaar: OpplysningDTO<Int>,
    val foedeland: OpplysningDTO<String>?,
    val doedsdato: OpplysningDTO<LocalDate>?,
    val adressebeskyttelse: OpplysningDTO<AdressebeskyttelseGradering>?,
    var bostedsadresse: List<OpplysningDTO<Adresse>>?,
    var deltBostedsadresse: List<OpplysningDTO<Adresse>>?,
    var kontaktadresse: List<OpplysningDTO<Adresse>>?,
    var oppholdsadresse: List<OpplysningDTO<Adresse>>?,
    val sivilstatus: OpplysningDTO<Sivilstatus>?,
    val sivilstand: List<OpplysningDTO<Sivilstand>>?,
    val statsborgerskap: OpplysningDTO<String>?,
    val pdlStatsborgerskap: OpplysningDTO<List<Statsborgerskap>>?,
    var utland: OpplysningDTO<Utland>?,
    var familieRelasjon: OpplysningDTO<FamilieRelasjon>?,
    var avdoedesBarn: List<Person>?,
    var vergemaalEllerFremtidsfullmakt: List<OpplysningDTO<VergemaalEllerFremtidsfullmakt>>?,
) {
    fun toPerson(): Person =
        Person(
            fornavn = fornavn.verdi,
            mellomnavn = mellomnavn?.verdi,
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
            sivilstand = sivilstand?.map { it.verdi },
            statsborgerskap = statsborgerskap?.verdi,
            pdlStatsborgerskap = pdlStatsborgerskap?.verdi,
            utland = utland?.verdi,
            familieRelasjon = familieRelasjon?.verdi,
            avdoedesBarn = avdoedesBarn?.map { it },
            avdoedesBarnUtenIdent = null,
            vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt?.map { it.verdi },
        )
}

open class OpplysningDTO<T>(
    val verdi: T,
    val opplysningsid: String?,
)
