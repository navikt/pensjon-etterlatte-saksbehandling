package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.domene.vedtak.Vedtak
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
    val beloep: BigDecimal? = null,
    val utbetalingId: UUID,
    val sakId: SakId,
    val erstatterId: UtbetalingslinjeId? = null,
    val utbetalingslinjetype: Utbetalingslinjetype? = null
)

enum class Utbetalingslinjetype {
    OPPHOER, UTBETALING
}


data class Utbetalingsperiode(
    val fra: LocalDate,
    val til: LocalDate? = null
) {
}


/*
Utbetalingsperioder som hører til siste utbetaling:
- inneholder én av disse dato for den første nye utbetalingslinjen?
    - hvis ja: erstatte denne
- Hvis nei -> må da finne tidligsteUtbetalingslinje blant eksisterende gjeldende linjer
    - erstatter så den

 */

data class UUID30(val value: String = UUID.randomUUID().toString().substring(0, 30))

/*
01.01 Vedtak: Jan 22 - Des 22: 10000 kr (opphøres eksplisitt)
01.02 Vedtak: Juni 22 - Feb 23: 7000 kr (opphøres implisitt)
01.03 Opphør: fom april 22

 */

