import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat

class OpplysningKanIkkeHentesUt : IllegalStateException()

fun hentFnrForeldre(familieRelasjon: FamilieRelasjon): List<String> {
    return familieRelasjon.foreldre?.map { it.value }
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentFnrForeldreAnsvar(familieRelasjon: FamilieRelasjon): List<String> {
    return familieRelasjon.ansvarligeForeldre?.map { it.value }
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