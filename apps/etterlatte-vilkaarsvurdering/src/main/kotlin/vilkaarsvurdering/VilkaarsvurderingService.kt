package no.nav.etterlatte.vilkaarsvurdering

import java.time.LocalDateTime

class VilkaarsvurderingService(val vilkaarsvurderingRepository: VilkaarsvurderingRepository) {

    fun hentVilkaarsvurdering(behandlingId: String): Vilkaarsvurdering {
        return vilkaarsvurderingRepository.hent(behandlingId) ?: opprettVilkaarsvurdering(behandlingId)
    }

    private fun opprettVilkaarsvurdering(behandlingId: String): Vilkaarsvurdering {
        val nyVilkaarsvurdering = Vilkaarsvurdering(behandlingId, vilkaarBarnepensjon())
        vilkaarsvurderingRepository.lagre(nyVilkaarsvurdering)
        return nyVilkaarsvurdering
    }

    fun oppdaterVilkaarsvurdering(behandlingId: String, oppdatertVilkaar: Vilkaar): Vilkaarsvurdering {
        return vilkaarsvurderingRepository.oppdater(behandlingId, oppdatertVilkaar)
    }
}

fun vilkaarBarnepensjon() = listOf(
    Vilkaar(VilkaarType.FORUTGAAENDE_MEDLEMSKAP),
    Vilkaar(VilkaarType.ALDER_BARN)
)

data class Vilkaarsvurdering(
    val behandlingId: String,
    val vilkaar: List<Vilkaar>
)

data class Vilkaar(
    val type: VilkaarType,
    val vurdering: VurdertResultat? = null
)

data class VurdertResultat(
    val resultat: Utfall,
    val kommentar: String?,
    val tidspunkt: LocalDateTime,
    val saksbehandler: String
)

enum class Utfall {
    OPPFYLT,
    IKKE_OPPFYLT
}

enum class VilkaarType(val beskrivelse: String) {
    ALDER_BARN("§18-1 Alder på barn"),
    FORUTGAAENDE_MEDLEMSKAP("§18-2 Forutgående medlemskap")
}