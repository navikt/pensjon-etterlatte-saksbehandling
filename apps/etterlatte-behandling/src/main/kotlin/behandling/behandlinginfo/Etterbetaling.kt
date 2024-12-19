package no.nav.etterlatte.behandling.behandlinginfo

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class Etterbetaling(
    val behandlingId: UUID,
    val frivilligSkattetrekk: Boolean?,
    val kilde: Grunnlagsopplysning.Saksbehandler,
) {
    companion object {
        fun fra(
            behandlingId: UUID,
            frivilligSkattetrekk: Boolean?,
            kilde: Grunnlagsopplysning.Saksbehandler,
        ): Etterbetaling =
            Etterbetaling(
                behandlingId = behandlingId,
                frivilligSkattetrekk = frivilligSkattetrekk,
                kilde = kilde,
            )
    }
}
