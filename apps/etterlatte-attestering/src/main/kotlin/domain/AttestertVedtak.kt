package no.nav.etterlatte.domain

import java.time.LocalDateTime

data class AttestertVedtak(
    val vedtakId: String,
    val attestantId: String?,
    val attestasjonstidspunkt: LocalDateTime?,
    val attestasjonsstatus: AttestasjonsStatus
)

enum class AttestasjonsStatus(s: String) {
    TIL_ATTESTERING("TIL_ATTESTERING"), IKKE_ATTESTERT("IKKE_ATTESTERT"), ATTESTERT("ATTESTERT")
}

