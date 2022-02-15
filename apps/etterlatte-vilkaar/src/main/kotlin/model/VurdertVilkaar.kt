package no.nav.etterlatte.vilkaar.model

import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning

enum class VilkaarVurderingsResultat {
    OPPFYLT, IKKE_OPPFYLT, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING;
}

data class VurdertVilkaar(
    val navn: String,
    val resultat: VilkaarVurderingsResultat,
    val basertPaaOpplysninger: List<VilkaarOpplysning<out Any>>,
)

