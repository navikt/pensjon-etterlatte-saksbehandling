package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.*

data class Behandling (
    val id: UUID,
    val sak: String,
    val grunnlag: List<Opplysning>,
    val vilkårsprøving: ObjectNode?,
    val beregning: ObjectNode?,
    val fastsatt: Boolean = false
)