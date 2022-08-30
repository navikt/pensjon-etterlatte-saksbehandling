import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

fun personTestData(
    fornavn: String = "Per",
    etternavn: String = "Persson",
    foedselsnummer: Foedselsnummer = Foedselsnummer.of("02071064479"),
    foedselsdato: LocalDate? = LocalDate.of(2010, 7, 2),
    foedselsaar: Int = 2010,
    foedeland: String? = "Norge",
    doedsdato: LocalDate? = null,
    adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.UGRADERT,
    bostedsadresse: List<Adresse>? = null,
    deltBostedsadresse: List<Adresse>? = null,
    kontaktadresse: List<Adresse>? = null,
    oppholdsadresse: List<Adresse>? = null,
    sivilstatus: Sivilstatus? = Sivilstatus.UGIFT,
    statsborgerskap: String? = "Norsk",
    utland: Utland? = null,
    familieRelasjon: FamilieRelasjon? = null,
    avdoedesBarn: List<Person>? = null,
    vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>? = null
): Person {
    return Person(
        fornavn = fornavn,
        etternavn = etternavn,
        foedselsnummer = foedselsnummer,
        foedselsdato = foedselsdato,
        foedselsaar = foedselsaar,
        foedeland = foedeland,
        doedsdato = doedsdato,
        adressebeskyttelse = adressebeskyttelse,
        bostedsadresse = bostedsadresse,
        deltBostedsadresse = deltBostedsadresse,
        kontaktadresse = kontaktadresse,
        oppholdsadresse = oppholdsadresse,
        sivilstatus = sivilstatus,
        statsborgerskap = statsborgerskap,
        utland = utland,
        familieRelasjon = familieRelasjon,
        avdoedesBarn = avdoedesBarn,
        vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt
    )
}