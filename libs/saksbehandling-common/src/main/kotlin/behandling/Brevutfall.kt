package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class Brevutfall(
    val behandlingId: UUID,
    val aldersgruppe: Aldersgruppe?,
    val lavEllerIngenInntekt: LavEllerIngenInntekt?,
    val kilde: Grunnlagsopplysning.Kilde,
)

enum class Aldersgruppe {
    OVER_18,
    UNDER_18,
}

enum class LavEllerIngenInntekt {
    JA,
    NEI,
}
