package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class BrevutfallDto(
    val behandlingId: UUID?,
    val aldersgruppe: Aldersgruppe?,
    val feilutbetaling: Feilutbetaling?,
    val kilde: Grunnlagsopplysning.Kilde?,
)
