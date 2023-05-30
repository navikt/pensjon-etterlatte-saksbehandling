package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.common.UUID30
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class VedtakId(val value: Long)
data class SakId(val value: Long)
data class BehandlingId(val value: UUID, val shortValue: UUID30)
data class UtbetalingslinjeId(val value: Long)
data class Foedselsnummer(val value: String)
data class NavIdent(val value: String)

enum class UtbetalingStatus {
    GODKJENT, GODKJENT_MED_FEIL, AVVIST, FEILET, SENDT, MOTTATT
}

enum class Utbetalingslinjetype {
    OPPHOER, UTBETALING
}

data class PeriodeForUtbetaling(
    val fra: LocalDate,
    val til: LocalDate? = null
)

data class Kvittering(
    val oppdrag: Oppdrag,
    val alvorlighetsgrad: String,
    val beskrivelse: String? = null,
    val kode: String? = null
)

data class Utbetaling(
    val id: UUID,
    val sakId: SakId,
    val sakType: Saktype,
    val behandlingId: BehandlingId,
    val vedtakId: VedtakId,
    val opprettet: Tidspunkt,
    val endret: Tidspunkt,
    val avstemmingsnoekkel: Tidspunkt,
    val stoenadsmottaker: Foedselsnummer,
    val saksbehandler: NavIdent,
    val saksbehandlerEnhet: String? = null,
    val attestant: NavIdent,
    val attestantEnhet: String? = null,
    val vedtak: Utbetalingsvedtak,
    val oppdrag: Oppdrag? = null,
    val kvittering: Kvittering? = null,
    val utbetalingslinjer: List<Utbetalingslinje>,
    val utbetalingshendelser: List<Utbetalingshendelse>
) {
    fun status() =
        utbetalingshendelser.minByOrNull { it.status }?.status ?: UtbetalingStatus.MOTTATT
}

data class Utbetalingslinje(
    val id: UtbetalingslinjeId,
    val type: Utbetalingslinjetype,
    val utbetalingId: UUID,
    val erstatterId: UtbetalingslinjeId? = null,
    val opprettet: Tidspunkt,
    val sakId: SakId,
    val periode: PeriodeForUtbetaling,
    val beloep: BigDecimal? = null,
    val klassifikasjonskode: OppdragKlassifikasjonskode
)

enum class OppdragKlassifikasjonskode(private val oppdragVerdi: String) {
    BARNEPENSJON_OPTP("BARNEPENSJON-OPTP"),
    OMSTILLINGSTOENAD_OPTP("OMSTILLINGOR");

    override fun toString(): String {
        return oppdragVerdi
    }

    companion object {
        fun fraString(string: String): OppdragKlassifikasjonskode = when (string) {
            "BARNEPENSJON-OPTP" -> BARNEPENSJON_OPTP
            "BARNEPENSJON_OPTP" -> BARNEPENSJON_OPTP
            "OMSTILLINGOR" -> OMSTILLINGSTOENAD_OPTP
            else -> throw IllegalArgumentException("$string er ikke en OppgragKlassifikasjonskode!")
        }
    }
}

data class Utbetalingshendelse(
    val id: UUID = UUID.randomUUID(),
    val utbetalingId: UUID,
    val tidspunkt: Tidspunkt = Tidspunkt.now(),
    val status: UtbetalingStatus
)