package no.nav.etterlatte.domain

import no.trygdeetaten.skjema.oppdrag.Oppdrag

enum class UtbetalingsoppdragStatus {
    MOTTATT,
    SENDT,
    GODKJENT,
    FEILET
}

data class Utbetalingsoppdrag(
    val id: Int,
    val vedtakId: String,
    val behandlingId: String,
    val sakId: String,
    val status: UtbetalingsoppdragStatus,
    val vedtak: Vedtak,
    val oppdrag: Oppdrag,
    val oppdragId: String? = null,
    val kvittering: Oppdrag? = null
)
