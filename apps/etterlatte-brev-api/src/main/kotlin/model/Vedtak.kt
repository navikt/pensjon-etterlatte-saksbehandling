package model

import java.time.LocalDate

data class Vedtak(
    val vedtakId: String,
    val type: VedtakType,
    val status: String,
    val dato: LocalDate,
    val gjelderFom: LocalDate,
    val sum: Double,
    val kontonummer: String,
    val virkningsdato: LocalDate,
    val saksnummer: String,
    val barn: Barn,
    val avdoed: Avdoed,
    val vilkaar: List<String>
)

enum class VedtakType {
    INNVILGELSE,
    AVSLAG,
    REVURDERING
}

data class Barn(val navn: String, val fnr: String)
data class Avdoed(val navn: String, val doedsdato: LocalDate)
