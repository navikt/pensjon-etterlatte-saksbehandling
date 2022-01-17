package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.Opplysning
import java.util.*

data class Behandling (
    val id: UUID,
    val sak: Long,
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