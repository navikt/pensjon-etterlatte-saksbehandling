package model

import java.time.LocalDate

data class FattetVedtak(
    val type: VedtakType,
    val dato: LocalDate,
    val gjelderFom: LocalDate,
    val sum: Double,
)

enum class VedtakType {
    INNVILGELSE,
    AVSLAG,
    REVURDERING
}
