package no.nav.etterlatte.vilkaar.model

import java.time.LocalDate


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

class VurdertVilkaar(
    val resultat: Tidslinje<VilkaarVurderingsResultat>,
    val basertPaaOpplysninger: List<Opplysning>,
    val navn: String
){
    fun serialize(): VurdertVilkaarJson{
        return VurdertVilkaarJson(resultat.knekkpunkt.map { TidslinjePeriodeJson(it.first, it.second) }, basertPaaOpplysninger, navn)
    }

}

data class TidslinjePeriodeJson(
    val fom:LocalDate,
    val resultat: Any
)

data class VurdertVilkaarJson(
    val tidslinje: List<TidslinjePeriodeJson>,
    val basertPaaOpplysninger: List<Opplysning>,
    val navn: String
)
