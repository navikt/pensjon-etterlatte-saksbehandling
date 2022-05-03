package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.*

data class Grunnlag(
    val id: UUID,
    val saksId: Long,
    val grunnlag: List<Grunnlagsopplysning<ObjectNode>>,
    //val gyldighetsprøving: GyldighetsResultat?,
    //val vilkårsprøving: VilkaarResultat?,
    //val beregning: Beregning?,
   // val fastsatt: Boolean = false,
    //val avbrutt: Boolean = false
)