package no.nav.etterlatte.domain

import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.time.LocalDateTime

enum class UtbetalingsoppdragStatus {
    SENDT,
    GODKJENT,
    GODKJENT_MED_FEIL,
    AVVIST,
    FEILET,
}

data class Utbetalingsoppdrag(
    val id: Int,
    val vedtakId: String,
    val behandlingId: String,
    val sakId: String,
    val status: UtbetalingsoppdragStatus,
    val vedtak: Vedtak,
    val opprettetTidspunkt: LocalDateTime,
    val endret: LocalDateTime,
    val avstemmingsnoekkel: LocalDateTime,
    val foedselsnummer: String,
    val utgaaendeOppdrag: Oppdrag,
    val oppdragKvittering: Oppdrag? = null,
    val beskrivelseOppdrag: String? = null,
    val feilkodeOppdrag: String? = null,
    val meldingKodeOppdrag: String? = null // 00, 04, 08, 12
)
