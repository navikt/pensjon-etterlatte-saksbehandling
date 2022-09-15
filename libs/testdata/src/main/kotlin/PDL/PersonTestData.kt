import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.ADRESSEBESKYTTELSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.DELTBOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.DOEDSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FAMILIERELASJON
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.KONTAKTADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.OPPHOLDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.SIVILSTATUS
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.STATSBORGERSKAP
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.UTLAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.VERGEMAALELLERFREMTIDSFULLMAKT
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import java.time.LocalDate

private inline fun <reified T>Map<Opplysningstyper, Opplysning<JsonNode>>.hentOpplysning(
    opplysningstype: Opplysningstyper
): T? {
    val opplysning = this[opplysningstype] as Opplysning.Konstant<T>? ?: return null
    return objectMapper.readValue(opplysning.verdi.toString(), object : TypeReference<T>() {})
}

fun personTestData(
    opplysningsmap: Map<Opplysningstyper, Opplysning<JsonNode>>
): Person = Person(
    fornavn = opplysningsmap.hentOpplysning<Navn>(NAVN)!!.fornavn,
    etternavn = opplysningsmap.hentOpplysning<Navn>(NAVN)!!.etternavn,
    foedselsnummer = opplysningsmap.hentOpplysning(FOEDSELSNUMMER)!!,
    foedselsdato = opplysningsmap.hentOpplysning(FOEDSELSDATO),
    foedselsaar = opplysningsmap.hentOpplysning<LocalDate>(FOEDSELSDATO)!!.year,
    foedeland = opplysningsmap.hentOpplysning(FOEDELAND),
    doedsdato = opplysningsmap.hentOpplysning(DOEDSDATO),
    adressebeskyttelse = opplysningsmap.hentOpplysning(ADRESSEBESKYTTELSE),
    bostedsadresse = opplysningsmap.hentOpplysning(BOSTEDSADRESSE),
    deltBostedsadresse = opplysningsmap.hentOpplysning(DELTBOSTEDSADRESSE),
    kontaktadresse = opplysningsmap.hentOpplysning(KONTAKTADRESSE),
    oppholdsadresse = opplysningsmap.hentOpplysning(OPPHOLDSADRESSE),
    sivilstatus = opplysningsmap.hentOpplysning(SIVILSTATUS),
    statsborgerskap = opplysningsmap.hentOpplysning(STATSBORGERSKAP),
    utland = opplysningsmap.hentOpplysning(UTLAND),
    familieRelasjon = opplysningsmap.hentOpplysning(FAMILIERELASJON),
    avdoedesBarn = opplysningsmap.hentOpplysning(AVDOEDESBARN),
    vergemaalEllerFremtidsfullmakt = opplysningsmap.hentOpplysning(VERGEMAALELLERFREMTIDSFULLMAKT)
)