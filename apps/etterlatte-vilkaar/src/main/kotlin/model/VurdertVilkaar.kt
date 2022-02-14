package no.nav.etterlatte.vilkaar.model

import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning


enum class VilkaarVurderingsResultat {
    OPPFYLT, IKKE_OPPFYLT, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING;

    companion object{
        val ogMatrise = arrayOf(
            arrayOf(OPPFYLT, IKKE_OPPFYLT, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING),
            arrayOf(IKKE_OPPFYLT, IKKE_OPPFYLT, IKKE_OPPFYLT),
            arrayOf(KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, IKKE_OPPFYLT, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING)
        )
        val ellerMatrise = arrayOf(
            arrayOf(OPPFYLT, OPPFYLT, OPPFYLT),
            arrayOf(OPPFYLT, IKKE_OPPFYLT, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING),
            arrayOf(OPPFYLT, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING)
        )
    }

    operator fun plus(other: VilkaarVurderingsResultat): VilkaarVurderingsResultat{
        return ellerMatrise[ordinal][other.ordinal]
    }
    operator fun times(other: VilkaarVurderingsResultat): VilkaarVurderingsResultat{
        return ogMatrise[ordinal][other.ordinal]
    }
}

data class VurdertVilkaar(
    val navn: String,
    val resultat: VilkaarVurderingsResultat,
    val basertPaaOpplysninger: List<VilkaarOpplysning<out Any>>,
)

