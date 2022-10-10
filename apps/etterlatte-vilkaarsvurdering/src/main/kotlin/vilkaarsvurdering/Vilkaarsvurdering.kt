package no.nav.etterlatte.vilkaarsvurdering

import java.time.LocalDateTime
import java.util.*

data class Vilkaarsvurdering(
    val behandlingId: UUID,
    val payload: String,
    val vilkaar: List<Vilkaar>,
    val resultat: VilkaarsvurderingResultat? = null
)

data class Vilkaar(
    val type: VilkaarType,
    val paragraf: Paragraf,
    val vurdering: VurdertResultat? = null,
    val unntaksvilkaar: List<Vilkaar>? = null
)

enum class VilkaarType {
    FORMAAL,
    FORUTGAAENDE_MEDLEMSKAP,
    FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MOTTOK_PENSJON_ELLER_UFOERETRYGD,
    FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR,
    FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_MEDLEM_ETTER_16_AAR,
    FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_HALV_MINSTEPENSJON,
    FORTSATT_MEDLEMSKAP,
    FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRE_MINST_20_AAR_SAMLET_BOTID,
    FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON,
    FORTSATT_MEDLEMSKAP_UNNTAK_FORELDRELOEST_BARN_I_KULL_MEDLEM_TRYGDEN,
    ALDER_BARN,
    ALDER_BARN_UNNTAK_UTDANNING,
    DOEDSFALL_FORELDER,
    YRKESSKADE_AVDOED
}

data class Paragraf(
    val paragraf: String,
    val ledd: Int? = null,
    val bokstav: String? = null,
    val tittel: String,
    val lenke: String? = null,
    val lovtekst: String
)

data class VurdertResultat(
    val resultat: Utfall,
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

data class VilkaarsvurderingResultat(
    val utfall: VilkaarsvurderingUtfall,
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

enum class Utfall {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT
}

enum class VilkaarsvurderingUtfall {
    OPPFYLT,
    IKKE_OPPFYLT
}

data class VurdertVilkaar(
    val vilkaarType: VilkaarType,
    val vurdertResultat: VurdertResultat
)