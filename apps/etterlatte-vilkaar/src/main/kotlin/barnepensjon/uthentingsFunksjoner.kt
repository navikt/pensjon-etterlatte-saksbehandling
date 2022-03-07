package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Bostedadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Oppholdadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters


fun hentFnrForeldre(foreldre: List<VilkaarOpplysning<Foreldre>>): List<String> {
    return foreldre.find { it.kilde.type == "pdl" }?.opplysning?.foreldre?.map { it.foedselsnummer.value }
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentSoekerFoedselsdato(foedselsdato: List<VilkaarOpplysning<Foedselsdato>>): LocalDate {
    return foedselsdato.find { it.kilde.type == "pdl" }?.opplysning?.foedselsdato
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentDoedsdato(doedsdato: List<VilkaarOpplysning<Doedsdato>>): Doedsdato {
    return doedsdato.find { it.kilde.type == "pdl" }?.opplysning
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentVirkningsdato(doedsdato: List<VilkaarOpplysning<Doedsdato>>): LocalDate {
    val doedsdato = doedsdato.find { it.kilde.type == "pdl" }?.opplysning?.doedsdato
    return doedsdato?.with(TemporalAdjusters.firstDayOfNextMonth()) ?: throw OpplysningKanIkkeHentesUt()
}

fun hentUtenlandsopphold(
    utenlandsopphold: List<VilkaarOpplysning<Utenlandsopphold>>,
    kildetype: String
): Utenlandsopphold {
    val utenlandsopphold = utenlandsopphold.find { it.kilde.type == kildetype }?.opplysning
    return utenlandsopphold ?: throw OpplysningKanIkkeHentesUt()
}

fun hentUtenlandsadresseSoeknad(
    utenlandsadresse: List<VilkaarOpplysning<Utenlandsadresse>>
): Utenlandsadresse {
    val utenlandsadresse = utenlandsadresse.find { it.kilde.type == "privatperson" }?.opplysning
    return utenlandsadresse ?: throw OpplysningKanIkkeHentesUt()
}

fun hentBostedsadresse(
    bostedadresse: List<VilkaarOpplysning<Bostedadresse>>
): Bostedadresse {
    val bostedadresse = bostedadresse.find { it.kilde.type == "pdl" }?.opplysning
    return bostedadresse ?: throw OpplysningKanIkkeHentesUt()
}

fun hentOppholdadresse(
    oppholdadresse: List<VilkaarOpplysning<Oppholdadresse>>
): Oppholdadresse {
    val bostedadresse = oppholdadresse.find { it.kilde.type == "pdl" }?.opplysning
    return bostedadresse ?: throw OpplysningKanIkkeHentesUt()
}
