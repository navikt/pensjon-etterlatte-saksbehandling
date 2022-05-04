package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters


fun hentFnrForeldre(soeker: VilkaarOpplysning<Person>): List<Foedselsnummer> {
    return soeker.opplysning.familieRelasjon?.foreldre?.map { it }
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentFoedselsdato(person: VilkaarOpplysning<Person>): LocalDate {
    return person.opplysning.foedselsdato
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentDoedsdato(person: VilkaarOpplysning<Person>): LocalDate {
    return person.opplysning.doedsdato
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentVirkningsdato(person: VilkaarOpplysning<Person>): LocalDate {
    val doedsdato = person.opplysning.doedsdato
    return doedsdato?.with(TemporalAdjusters.firstDayOfNextMonth()) ?: throw OpplysningKanIkkeHentesUt()
}

fun hentAdresser(
    person: VilkaarOpplysning<Person>
): Adresser {
    val bostedadresse = person.opplysning.bostedsadresse
    val oppholdadresse = person.opplysning.oppholdsadresse
    val kontaktadresse = person.opplysning.kontaktadresse

    val adresser = Adresser(bostedadresse, oppholdadresse, kontaktadresse)
    val ingenAdresser = bostedadresse?.isEmpty() == true && oppholdadresse?.isEmpty() == true && kontaktadresse?.isEmpty() == true

    return if (ingenAdresser) {
        throw OpplysningKanIkkeHentesUt()
    } else {
        adresser
    }
}

fun hentBostedsAdresser(
    person: VilkaarOpplysning<Person>
): List<Adresse> {
    return person.opplysning.bostedsadresse
        ?: throw OpplysningKanIkkeHentesUt()
}

