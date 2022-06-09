package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.common.toUUID30
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Attestasjon
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Behandling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.BehandlingId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.BehandlingType
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kvittering
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.NavIdent
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Periode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.PeriodeForUtbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Sak
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingshendelse
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinjetype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsperiode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingsperiodeType
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsvedtak
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.VedtakFattet
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.VedtakId
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

object TestHelper

fun readFile(file: String) = TestHelper::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")


fun utbetalingsvedtak(
    vedtakId: Long = 1,
    utbetalingsperioder: List<Utbetalingsperiode> = listOf(
        Utbetalingsperiode(
            id = 1,
            periode = Periode(fom = YearMonth.of(2022, 1), null),
            beloep = BigDecimal.valueOf(2000),
            type = UtbetalingsperiodeType.UTBETALING
        )
    )
) = Utbetalingsvedtak(
    vedtakId = vedtakId,
    behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.FORSTEGANGSBEHANDLING
    ),
    sak = Sak(
        id = 1,
        ident = "12345678913",
    ),
    vedtakFattet = VedtakFattet(
        ansvarligSaksbehandler = "12345678",
    ),
    attestasjon = Attestasjon(
        attestant = "87654321",
    ),
    pensjonTilUtbetaling = utbetalingsperioder,
)

fun oppdrag(utbetaling: Utbetaling, foerstegangsbehandling: Boolean = true) =
    OppdragMapper.oppdragFraUtbetaling(utbetaling, foerstegangsbehandling)

fun kvittering(oppdragMedKvittering: Oppdrag) =
    Kvittering(
        oppdrag = oppdragMedKvittering,
        beskrivelse = oppdragMedKvittering.mmel.alvorlighetsgrad,
        alvorlighetsgrad = oppdragMedKvittering.mmel.beskrMelding,
        kode = oppdragMedKvittering.mmel.kodeMelding
    )

fun oppdragMedGodkjentKvittering(
    utbetaling: Utbetaling = utbetaling(
        vedtakId = 1,
        utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.GODKJENT))
    )
) = oppdrag(utbetaling).apply {
    mmel = Mmel().apply {
        alvorlighetsgrad = "00"
    }
}

fun oppdragMedFeiletKvittering(
    utbetaling: Utbetaling = utbetaling(
        vedtakId = 1,
        utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.FEILET))
    )
) = oppdrag(utbetaling).apply {
    mmel = Mmel().apply {
        alvorlighetsgrad = "12"
        kodeMelding = "KodeMelding"
        beskrMelding = "Beskrivelse"
    }
}

fun utbetaling(
    id: UUID = UUID.randomUUID(),
    sakId: SakId = SakId(1),
    vedtakId: Long = 1,
    avstemmingsnoekkel: Tidspunkt = Tidspunkt.now(),
    opprettet: Tidspunkt = Tidspunkt.now(),
    utbetalingslinjeId: Long = 1L,
    periodeFra: LocalDate = LocalDate.parse("2022-01-01"),
    utbetalingslinjer: List<Utbetalingslinje> = listOf(
        utbetalingslinje(
            id,
            sakId,
            utbetalingslinjeId,
            periodeFra = periodeFra
        )
    ),
    kvittering: Kvittering? = null,
    utbetalingshendelser: List<Utbetalingshendelse> = listOf(
        utbetalingshendelse(
            utbetalingId = id,
            tidspunkt = opprettet
        )
    )
) =
    Utbetaling(
        id = id,
        vedtakId = VedtakId(vedtakId),
        behandlingId = UUID.randomUUID().let { BehandlingId(it, it.toUUID30()) },
        sakId = sakId,
        vedtak = utbetalingsvedtak(vedtakId),
        opprettet = opprettet,
        endret = Tidspunkt.now(),
        avstemmingsnoekkel = avstemmingsnoekkel,
        stoenadsmottaker = Foedselsnummer("12345678903"),
        saksbehandler = NavIdent("12345678"),
        attestant = NavIdent("87654321"),
        utbetalingslinjer = utbetalingslinjer,
        kvittering = kvittering,
        utbetalingshendelser = utbetalingshendelser
    )

fun utbetalingslinje(
    utbetalingId: UUID = UUID.randomUUID(),
    sakId: SakId = SakId(1),
    utbetalingslinjeId: Long = 1,
    type: Utbetalingslinjetype = Utbetalingslinjetype.UTBETALING,
    beloep: BigDecimal? = BigDecimal.valueOf(10000),
    periodeFra: LocalDate = LocalDate.parse("2022-01-01")
): Utbetalingslinje =
    Utbetalingslinje(
        id = UtbetalingslinjeId(utbetalingslinjeId),
        type = type,
        utbetalingId = utbetalingId,
        erstatterId = null,
        opprettet = Tidspunkt.now(),
        sakId = sakId,
        periode = PeriodeForUtbetaling(
            fra = periodeFra,
        ),
        beloep = beloep,
    )

fun utbetalingMedOpphoer() = utbetaling(
    utbetalingslinjer = listOf(
        utbetalingslinje(
            utbetalingId = UUID.randomUUID(),
            sakId = SakId(1),
            utbetalingslinjeId = 1,
            type = Utbetalingslinjetype.OPPHOER,
            beloep = null
        )
    )
)

fun utbetalingshendelse(
    id: UUID = UUID.randomUUID(),
    utbetalingId: UUID = UUID.randomUUID(),
    tidspunkt: Tidspunkt = Tidspunkt.now(),
    status: UtbetalingStatus = UtbetalingStatus.GODKJENT
) = Utbetalingshendelse(id, utbetalingId, tidspunkt, status)