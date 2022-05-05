package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.time.LocalDateTime

enum class UtbetalingStatus {
    SENDT,
    GODKJENT,
    GODKJENT_MED_FEIL,
    AVVIST,
    FEILET,
}

data class Utbetaling(
    val id: Int,
    val vedtakId: String,
    val behandlingId: String,
    val sakId: String,
    val status: UtbetalingStatus,
    val vedtak: Vedtak,
    val opprettet: LocalDateTime,
    val endret: LocalDateTime,
    val avstemmingsnoekkel: LocalDateTime,
    val foedselsnummer: String,
    val utgaaendeOppdrag: Oppdrag,
    val oppdragKvittering: Oppdrag? = null,
    val beskrivelseOppdrag: String? = null,
    val feilkodeOppdrag: String? = null,
    val meldingKodeOppdrag: String? = null // 00, 04, 08, 12
)
