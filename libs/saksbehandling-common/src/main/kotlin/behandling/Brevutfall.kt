package no.nav.etterlatte.libs.common.behandling

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
    JA_INGEN_VARSEL_MOTREGNES,
    JA_INGEN_TK,
}

data class SendBrev(
    val sendBrev: Boolean,
)
