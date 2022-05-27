package no.nav.etterlatte.utbetaling.iverksetting.utbetaling


import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*


data class VedtakId(val value: Long)
data class SakId(val value: Long)
data class BehandlingId(val value: String)
data class UtbetalingslinjeId(val value: Long)
data class Foedselsnummer(val value: String)
data class NavIdent(val value: String)

enum class UtbetalingStatus {
    SENDT, GODKJENT, GODKJENT_MED_FEIL, AVVIST, FEILET,
}

enum class Utbetalingslinjetype {
    OPPHOER, UTBETALING
}

data class PeriodeForUtbetaling(
    val fra: LocalDate, val til: LocalDate? = null
)

data class Kvittering(
    val oppdrag: Oppdrag,
    val feilkode: String,
    val beskrivelse: String? = null,
    val meldingKode: String? = null,
)

data class Utbetaling(
    val id: UUID,
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val vedtakId: VedtakId,
    val status: UtbetalingStatus,
    val opprettet: Tidspunkt,
    val endret: Tidspunkt,
    val avstemmingsnoekkel: Tidspunkt,
    val stoenadsmottaker: Foedselsnummer,
    val saksbehandler: NavIdent,
    val attestant: NavIdent,
    val vedtak: Utbetalingsvedtak,
    val oppdrag: Oppdrag? = null,
    val kvittering: Kvittering? = null,
    val utbetalingslinjer: List<Utbetalingslinje>
)

data class Utbetalingslinje(
    val id: UtbetalingslinjeId,
    val type: Utbetalingslinjetype,
    val utbetalingId: UUID,
    val erstatterId: UtbetalingslinjeId? = null,
    val opprettet: Tidspunkt,
    val sakId: SakId,
    val periode: PeriodeForUtbetaling,
    val beloep: BigDecimal? = null,
)
