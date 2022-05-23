package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.domene.vedtak.Attestasjon
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.domene.vedtak.BehandlingType
import no.nav.etterlatte.domene.vedtak.Periode
import no.nav.etterlatte.domene.vedtak.Sak
import no.nav.etterlatte.domene.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.domene.vedtak.VedtakFattet
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.BehandlingId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kvittering
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.NavIdent
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinjetype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsperiode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.VedtakId
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*

object TestHelper

fun readFile(file: String) = TestHelper::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")

fun vedtak(vedtakId: Long) = Vedtak(
    vedtakId = vedtakId,
    behandling = Behandling(
        id = UUID.randomUUID(),
        type = BehandlingType.FORSTEGANGSBEHANDLING
    ),
    sak = Sak(
        id = 1,
        ident = "12345678913",
        sakType = ""
    ),
    type = VedtakType.INNVILGELSE,
    virk = Periode(YearMonth.of(2022, 1), YearMonth.of(2022, 2)),
    grunnlag = emptyList(),
    vedtakFattet = VedtakFattet(
        ansvarligSaksbehandler = "12345678",
        ansvarligEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    attestasjon = Attestasjon(
        attestant = "87654321",
        attesterendeEnhet = "123",
        tidspunkt = ZonedDateTime.now()
    ),
    pensjonTilUtbetaling = listOf(
        no.nav.etterlatte.domene.vedtak.Utbetalingsperiode(
            id = 1,
            periode = Periode(fom = YearMonth.of(2022, 1), null),
            beloep = BigDecimal.valueOf(2000),
            type = UtbetalingsperiodeType.UTBETALING
        )
    ),
    avkorting = null,
    beregning = null,
    vilkaarsvurdering = null
)

fun oppdrag(utbetaling: Utbetaling, foerstegangsbehandling: Boolean = true) =
    OppdragMapper.oppdragFraUtbetaling(utbetaling, foerstegangsbehandling)

fun kvittering(oppdragMedKvittering: Oppdrag) =
    Kvittering(
        oppdrag = oppdragMedKvittering,
        beskrivelse = oppdragMedKvittering.mmel.alvorlighetsgrad,
        feilkode = oppdragMedKvittering.mmel.beskrMelding,
        meldingKode = oppdragMedKvittering.mmel.kodeMelding
    )

fun oppdragMedGodkjentKvittering(utbetaling: Utbetaling = utbetaling(vedtakId = 1)) = oppdrag(utbetaling).apply {
    mmel = Mmel().apply {
        alvorlighetsgrad = "00"
    }
}

fun oppdragMedFeiletKvittering(utbetaling: Utbetaling = utbetaling(vedtakId = 1)) = oppdrag(utbetaling).apply {
    mmel = Mmel().apply {
        alvorlighetsgrad = "12"
        kodeMelding = "KodeMelding"
        beskrMelding = "Beskrivelse"
    }
}

fun utbetaling(
    id: UUID = UUID.randomUUID(),
    sakId: SakId = SakId(1),
    status: UtbetalingStatus = UtbetalingStatus.GODKJENT,
    vedtakId: Long = 1,
    avstemmingsnoekkel: Tidspunkt = Tidspunkt.now(),
    opprettet: Tidspunkt = Tidspunkt.now(),
    utbetalingslinjeId: Long = 1L,
    utbetalingslinjer: List<Utbetalingslinje> = listOf(utbetalingslinje(id, sakId, utbetalingslinjeId)),
    kvittering: Kvittering? = null
) =
    Utbetaling(
        id = id,
        vedtakId = VedtakId(vedtakId),
        behandlingId = BehandlingId("1"),
        sakId = sakId,
        status = status,
        vedtak = vedtak(vedtakId),
        opprettet = opprettet,
        endret = Tidspunkt.now(),
        avstemmingsnoekkel = avstemmingsnoekkel,
        stoenadsmottaker = Foedselsnummer("12345678903"),
        saksbehandler = NavIdent("12345678"),
        attestant = NavIdent("87654321"),
        utbetalingslinjer = utbetalingslinjer,
        kvittering = kvittering
    )

fun utbetalingslinje(
    utbetalingId: UUID,
    sakId: SakId,
    utbetalingslinjeId: Long
): Utbetalingslinje =
    Utbetalingslinje(
        id = UtbetalingslinjeId(utbetalingslinjeId),
        type = Utbetalingslinjetype.UTBETALING,
        utbetalingId = utbetalingId,
        erstatterId = null,
        opprettet = Tidspunkt.now(),
        sakId = sakId,
        periode = Utbetalingsperiode(
            fra = LocalDate.parse("2022-01-01"),
        ),
        beloep = BigDecimal.valueOf(10000),
    )
