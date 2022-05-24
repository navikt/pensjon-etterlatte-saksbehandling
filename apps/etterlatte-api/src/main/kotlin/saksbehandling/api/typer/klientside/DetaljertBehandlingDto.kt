package no.nav.etterlatte.saksbehandling.api.typer.klientside

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.util.*

data class DetaljertBehandlingDto(
    val id: UUID,
    val sak: Long,
    val grunnlag: List<Grunnlagsopplysning<ObjectNode>>,
    val gyldighetsprøving: GyldighetsResultat?,
    val vilkårsprøving: VilkaarResultat?,
    val kommerSoekerTilgode: VilkaarResultat?,
    val beregning: BeregningsResultat?,
    val fastsatt: Boolean = false
)