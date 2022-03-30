package no.nav.etterlatte.libs.common.vikaar

enum class VilkaarVurderingsResultat {
    OPPFYLT, IKKE_OPPFYLT, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING;
}

data class VurdertVilkaar(
    val navn: Vilkaartyper,
    val resultat: VilkaarVurderingsResultat,
    val kriterier: List<Kriterie>,
)

data class VilkaarResultat(
    val resultat: VilkaarVurderingsResultat?,
    val vilkaar: List<VurdertVilkaar>?
)

