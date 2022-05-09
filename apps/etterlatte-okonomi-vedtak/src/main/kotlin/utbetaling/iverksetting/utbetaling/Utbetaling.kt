package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.trygdeetaten.skjema.oppdrag.Oppdrag

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
    val opprettet: Tidspunkt,
    val endret: Tidspunkt,
    val avstemmingsnoekkel: Tidspunkt,
    val foedselsnummer: String,
    val utgaaendeOppdrag: Oppdrag,
    val kvitteringOppdrag: Oppdrag? = null,
    val kvitteringBeskrivelse: String? = null,
    val kvitteringFeilkode: String? = null,
    val kvitteringMeldingKode: String? = null // 00, 04, 08, 12
)
