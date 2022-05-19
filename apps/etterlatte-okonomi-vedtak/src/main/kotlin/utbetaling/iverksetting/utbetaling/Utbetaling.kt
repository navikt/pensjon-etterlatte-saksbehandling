package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*


data class VedtakId(val value: String)
data class SakId(val value: Long)
data class BehandlingId(val value: String)
data class UtbetalingslinjeId(val value: Long)
data class Foedselsnummer(val value: String)
data class NavIdent(val value: String)

enum class UtbetalingStatus {
    SENDT,
    GODKJENT,
    GODKJENT_MED_FEIL,
    AVVIST,
    FEILET,
}

data class Utbetalinger(
    val sakid: SakId,
    val utbetalinger: List<Utbetaling>,
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
    val vedtak: Vedtak,
    val oppdrag: Oppdrag? = null,
    val kvittering: Oppdrag? = null,
    val kvitteringBeskrivelse: String? = null,
    val kvitteringFeilkode: String? = null,
    val kvitteringMeldingKode: String? = null,
    val utbetalingslinjer: List<Utbetalingslinje>
)

data class Utbetalingslinje(
    val id: UtbetalingslinjeId,
    val opprettet: Tidspunkt,
    val periode: Utbetalingsperiode,
    val beloep: BigDecimal,
    val utbetalingId: UUID,
    val sakId: SakId,
    val erstatterId: String? = null,
    val endring: Endring? = null
)

enum class Endring {
    OPPHOER
}

data class Utbetalingsperiode(
    val fra: LocalDate,
    val til: LocalDate? = null
)

data class UUID30(val value: String = UUID.randomUUID().toString().substring(0, 30))

/*
01.01 Vedtak: Jan 22 - Des 22: 10000 kr (opphøres eksplisitt)
01.02 Vedtak: Juni 22 - Feb 23: 7000 kr (opphøres implisitt)
01.03 Opphør: fom april 22

 */

