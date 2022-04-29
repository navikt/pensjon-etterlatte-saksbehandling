package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.time.LocalDateTime
import java.util.*

data class DetaljertGrunnlag(
    val id: UUID,
    val sak: Long,
    val grunnlag: List<Grunnlagsopplysning<ObjectNode>>,
)