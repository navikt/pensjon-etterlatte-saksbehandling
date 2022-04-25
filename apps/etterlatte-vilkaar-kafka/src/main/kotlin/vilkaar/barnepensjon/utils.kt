package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDateTime

class OpplysningKanIkkeHentesUt : IllegalStateException()

fun setVikaarVurderingFraKriterier(kriterie: List<Kriterie>): VurderingsResultat {
    val resultat = kriterie.map { it.resultat }
    return hentVurdering(resultat)
}

fun setVilkaarVurderingFraVilkaar(vilkaar: List<VurdertVilkaar>): VurderingsResultat {
    val resultat = vilkaar.map { it.resultat }
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

fun hentSisteVurderteDato(vilkaar: List<VurdertVilkaar>): LocalDateTime {
    val datoer = vilkaar.map { it.vurdertDato }
    return datoer.maxOf { it }
}

fun vurderOpplysning(vurdering: () -> Boolean): VurderingsResultat = try {
    if (vurdering()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
} catch (ex: OpplysningKanIkkeHentesUt) {
    VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
}


fun opplysningsGrunnlagNull(
    kriterietype: Kriterietyper,
    opplysningsGrunnlag: List<Kriteriegrunnlag<out Any>>
): Kriterie {
    return Kriterie(
        kriterietype,
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
        opplysningsGrunnlag
    )
}

