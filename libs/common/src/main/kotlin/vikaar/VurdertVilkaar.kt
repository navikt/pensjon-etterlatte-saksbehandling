package no.nav.etterlatte.libs.common.vikaar
import java.time.LocalDateTime

enum class VurderingsResultat {
    OPPFYLT, IKKE_OPPFYLT, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING;
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
)

data class VilkaarResultat(
    val resultat: VurderingsResultat?,
    val vilkaar: List<VurdertVilkaar>?,
    val vurdertDato: LocalDateTime
)