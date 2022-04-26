import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat

class OpplysningKanIkkeHentesUt : IllegalStateException()

fun hentFnrForeldre(soeker: VilkaarOpplysning<Person>): List<Foedselsnummer> {
    return soeker.opplysning.familieRelasjon?.foreldre?.map { it }
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentFnrForeldreAnsvar(soeker: VilkaarOpplysning<Person>): List<Foedselsnummer> {
    return soeker.opplysning.familieRelasjon?.ansvarligeForeldre?.map { it }
        ?: throw OpplysningKanIkkeHentesUt()
}

fun vurderOpplysning(vurdering: () -> Boolean): VurderingsResultat = try {
    if (vurdering()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
} catch (ex: OpplysningKanIkkeHentesUt) {
    VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
}

fun setVurdering(liste: List<VurdertGyldighet>): VurderingsResultat {
    val resultat = liste.map { it.resultat }
    return hentVurdering(resultat)
}

fun hentVurdering(resultat: List<VurderingsResultat>): VurderingsResultat {
    return if (resultat.all { it == VurderingsResultat.OPPFYLT }) {
        VurderingsResultat.OPPFYLT
    } else if (resultat.any { it == VurderingsResultat.IKKE_OPPFYLT }) {
        VurderingsResultat.IKKE_OPPFYLT
    } else {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }
}