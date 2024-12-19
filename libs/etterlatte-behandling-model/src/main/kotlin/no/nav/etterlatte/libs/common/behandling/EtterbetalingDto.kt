package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class EtterbetalingDto(
    val behandlingId: UUID?,
    val frivilligSkattetrekk: Boolean?,
    val kilde: Grunnlagsopplysning.Kilde?,
)
