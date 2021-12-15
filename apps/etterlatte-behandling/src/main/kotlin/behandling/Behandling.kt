package no.nav.etterlatte.behandling

import java.util.*

data class Behandling (
    val id: UUID,
    val sak: String,
    val grunnlag: List<Opplysning>,
    val vilkårsprøving: Vilkårsprøving?,
    val beregning: Beregning?,
    val fastsatt: Boolean = false
){
    val status get() = when {
        fastsatt -> "fastsatt"
        beregning != null -> "beregnet"
        vilkårsprøving != null -> "vilkårsprøvd"
        else -> "opprettet"
    }
}