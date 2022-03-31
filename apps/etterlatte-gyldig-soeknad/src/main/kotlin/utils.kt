import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat

class OpplysningKanIkkeHentesUt : IllegalStateException()

fun hentFnrAnsvarligeForeldre(soeker: VilkaarOpplysning<Person>): List<Foedselsnummer> {
    return soeker.opplysning.familieRelasjon?.foreldre?.map { it }
        ?: throw OpplysningKanIkkeHentesUt()
}

fun vurderOpplysning(vurdering: () -> Boolean): VurderingsResultat = try {
    if (vurdering()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
} catch (ex: OpplysningKanIkkeHentesUt) {
    VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
}

fun hentBostedsAdresser(
    person: VilkaarOpplysning<Person>
): List<Adresse> {
    return person.opplysning.bostedsadresse
        ?: throw OpplysningKanIkkeHentesUt()
}