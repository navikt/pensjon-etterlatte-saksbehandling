package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat

class OpplysningKanIkkeHentesUt : IllegalStateException()

fun setVikaarVurderingsResultat(kriterie: List<Kriterie>): VilkaarVurderingsResultat {
    val resultat = kriterie.map { it.resultat }
    return if (resultat.all { it == VilkaarVurderingsResultat.OPPFYLT }) {
        VilkaarVurderingsResultat.OPPFYLT
    } else if (resultat.any { it == VilkaarVurderingsResultat.IKKE_OPPFYLT }) {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    } else {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }
}


fun vurderOpplysning(vurdering: () -> Boolean): VilkaarVurderingsResultat = try {
    if (vurdering()) VilkaarVurderingsResultat.OPPFYLT else VilkaarVurderingsResultat.IKKE_OPPFYLT
} catch (ex: OpplysningKanIkkeHentesUt) {
    VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
}
