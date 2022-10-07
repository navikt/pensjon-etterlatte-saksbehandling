package no.nav.etterlatte.libs.common.vikaar

import java.time.LocalDateTime

enum class VurderingsResultat(val prioritet: Int) {
    OPPFYLT(1),
    KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING(2),
    IKKE_OPPFYLT(3)
}

enum class Utfall {
    OPPFYLT, BEHANDLE_I_PSYS, TRENGER_AVKLARING
}

data class VurdertVilkaar(
    val navn: Vilkaartyper,
    val resultat: VurderingsResultat,
    val utfall: Utfall?,
    val kriterier: List<Kriterie>,
    val vurdertDato: LocalDateTime
) {
    companion object {
        fun kanIkkeVurdere(type: Vilkaartyper): VurdertVilkaar {
            return VurdertVilkaar(
                type,
                VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
                null,
                emptyList(),
                LocalDateTime.now()
            )
        }
    }
}

data class VilkaarResultat(
    val resultat: VurderingsResultat?,
    val vilkaar: List<VurdertVilkaar>?,
    val vurdertDato: LocalDateTime
)