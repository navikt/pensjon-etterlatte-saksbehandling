package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class Brevutfall(
    val behandlingId: UUID,
    val aldersgruppe: Aldersgruppe?,
    val feilutbetaling: Feilutbetaling?,
    val kilde: Grunnlagsopplysning.Kilde,
)

enum class Aldersgruppe {
    OVER_18,
    UNDER_18,
}

data class Feilutbetaling(
    val valg: FeilutbetalingValg,
    val kommentar: String?,
)

enum class FeilutbetalingValg {
    NEI,
    JA_VARSEL,
    JA_INGEN_TK,
}

data class SendBrev(
    val sendBrev: Boolean,
)
