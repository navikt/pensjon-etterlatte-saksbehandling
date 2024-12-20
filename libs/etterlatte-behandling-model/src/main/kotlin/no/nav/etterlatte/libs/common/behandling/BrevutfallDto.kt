package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class BrevutfallDto(
    val behandlingId: UUID?,
    val opphoer: Boolean? = null,
    val aldersgruppe: Aldersgruppe?,
    val harEtterbetaling: Boolean?,
    val feilutbetaling: Feilutbetaling?,
    val frivilligSkattetrekk: Boolean?,
    val kilde: Grunnlagsopplysning.Kilde?,
)
