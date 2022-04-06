package no.nav.etterlatte.domain

import java.time.LocalDateTime

data class AttestertVedtak(
    val vedtakId: String,
    val attestantId: String?,
    val tidspunkt: LocalDateTime?,
    val status: AttestasjonsStatus
)

enum class AttestasjonsStatus() {
    TIL_ATTESTERING(), IKKE_ATTESTERT(), ATTESTERT()
}

