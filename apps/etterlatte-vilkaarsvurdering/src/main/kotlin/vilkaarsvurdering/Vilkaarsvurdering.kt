package no.nav.etterlatte.vilkaarsvurdering

import java.time.LocalDateTime

data class Vilkaarsvurdering(
    val behandlingId: String,
    val vilkaar: List<Vilkaar>
)

data class Vilkaar(
    val type: VilkaarType,
    val paragraf: Paragraf,
    val vurdering: VurdertResultat? = null
)

enum class VilkaarType {
    FORMAAL,
    FORUTGAAENDE_MEDLEMSKAP,
    FORTSATT_MEDLEMSKAP,
    ALDER_BARN,
    DOEDSFALL_FORELDER
}

data class Paragraf(
    val paragraf: String,
    val tittel: String,
    val lenke: String,
    val lovtekst: String
)

data class VurdertResultat(
    val resultat: Utfall,
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

enum class Utfall {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT
}

data class VurdertVilkaar(
    val vilkaarType: VilkaarType,
    val vurdertResultat: VurdertResultat
)