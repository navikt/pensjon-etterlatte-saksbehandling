package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.utbetaling.avstemming.Konsistensavstemming
import no.nav.etterlatte.utbetaling.avstemming.OppdragForKonsistensavstemming
import no.nav.etterlatte.utbetaling.avstemming.OppdragslinjeForKonsistensavstemming
import no.nav.etterlatte.utbetaling.common.toUUID30
import no.nav.etterlatte.utbetaling.grensesnittavstemming.UUIDBase64
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Attestasjon
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.BehandlingId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kjoereplan
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kvittering
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.NavIdent
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdragKlassifikasjonskode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Periode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.PeriodeForUtbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Sak
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
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
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

object TestHelper

fun readFile(file: String) =
    TestHelper::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")

fun utbetalingsvedtak(
    vedtakId: Long = 1,
    utbetalingsperioder: List<Utbetalingsperiode> =
        listOf(
            Utbetalingsperiode(
                id = 1,
                periode = Periode(fom = YearMonth.of(2022, 1), null),
                beloep = BigDecimal.valueOf(2000),
                type = UtbetalingsperiodeType.UTBETALING,
            ),
        ),
) = Utbetalingsvedtak(
    vedtakId = vedtakId,
    behandling =
        Behandling(
            id = UUID.randomUUID(),
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            revurderingsaarsak = null,
        ),
    sak =
        Sak(
            id = 1,
            ident = "12345678913",
            sakType = Saktype.BARNEPENSJON,
        ),
    vedtakFattet =
        VedtakFattet(
            ansvarligSaksbehandler = "12345678",
            ansvarligEnhet = "4819",
        ),
    attestasjon =
        Attestasjon(
            attestant = "87654321",
            attesterendeEnhet = "4819",
        ),
    pensjonTilUtbetaling = utbetalingsperioder,
)

fun oppdrag(
    utbetaling: Utbetaling,
    foerstegangsbehandling: Boolean = true,
) = OppdragMapper.oppdragFraUtbetaling(utbetaling, foerstegangsbehandling)

fun kvittering(oppdragMedKvittering: Oppdrag) =
    Kvittering(
        oppdrag = oppdragMedKvittering,
        beskrivelse = oppdragMedKvittering.mmel.alvorlighetsgrad,
        alvorlighetsgrad = oppdragMedKvittering.mmel.beskrMelding,
        kode = oppdragMedKvittering.mmel.kodeMelding,
    )

fun oppdragMedGodkjentKvittering(
    utbetaling: Utbetaling =
        utbetaling(
            vedtakId = 1,
            utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.GODKJENT)),
        ),
) = oppdrag(utbetaling).apply {
    mmel =
        Mmel().apply {
            alvorlighetsgrad = "00"
        }
}

fun oppdragMedFeiletKvittering(
    utbetaling: Utbetaling =
        utbetaling(
            vedtakId = 1,
            utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.FEILET)),
        ),
) = oppdrag(utbetaling).apply {
    mmel =
        Mmel().apply {
            alvorlighetsgrad = "12"
            kodeMelding = "KodeMelding"
            beskrMelding = "Beskrivelse"
        }
}

fun utbetaling(
    id: UUID = UUID.randomUUID(),
    sakId: SakId = SakId(1),
    sakType: Saktype? = Saktype.BARNEPENSJON,
    vedtakId: Long = 1,
    avstemmingsnoekkel: Tidspunkt = Tidspunkt.now(),
    opprettet: Tidspunkt = Tidspunkt.now(),
    utbetalingslinjeId: Long = 1L,
    periodeFra: LocalDate = LocalDate.parse("2022-01-01"),
    periodeTil: LocalDate? = null,
    utbetalingslinjer: List<Utbetalingslinje> =
        listOf(
            utbetalingslinje(
                id,
                sakId,
                utbetalingslinjeId,
                periodeFra = periodeFra,
                periodeTil = periodeTil,
                opprettet = opprettet,
            ),
        ),
    kvittering: Kvittering? = null,
    utbetalingshendelser: List<Utbetalingshendelse> =
        listOf(
            utbetalingshendelse(
                utbetalingId = id,
                tidspunkt = opprettet,
            ),
        ),
    behandlingId: UUID = UUID.randomUUID(),
) = Utbetaling(
    id = id,
    vedtakId = VedtakId(vedtakId),
    behandlingId = BehandlingId(behandlingId, behandlingId.toUUID30()),
    sakType = sakType ?: Saktype.BARNEPENSJON,
    sakId = sakId,
    vedtak = utbetalingsvedtak(vedtakId),
    opprettet = opprettet,
    endret = Tidspunkt.now(),
    avstemmingsnoekkel = avstemmingsnoekkel,
    stoenadsmottaker = Foedselsnummer("12345678903"),
    saksbehandler = NavIdent("12345678"),
    saksbehandlerEnhet = "4819",
    attestant = NavIdent("87654321"),
    attestantEnhet = "4819",
    utbetalingslinjer = utbetalingslinjer,
    kvittering = kvittering,
    utbetalingshendelser = utbetalingshendelser,
)

fun utbetalingslinje(
    utbetalingId: UUID = UUID.randomUUID(),
    sakId: SakId = SakId(1),
    utbetalingslinjeId: Long = 1,
    type: Utbetalingslinjetype = Utbetalingslinjetype.UTBETALING,
    erstatter: Long? = null,
    beloep: BigDecimal? = BigDecimal.valueOf(10000),
    periodeFra: LocalDate = LocalDate.parse("2022-01-01"),
    periodeTil: LocalDate? = null,
    opprettet: Tidspunkt = Tidspunkt.now(),
    klassifikasjonskode: OppdragKlassifikasjonskode = OppdragKlassifikasjonskode.BARNEPENSJON_OPTP,
    kjoereplan: Kjoereplan = Kjoereplan.MED_EN_GANG,
): Utbetalingslinje =
    Utbetalingslinje(
        id = UtbetalingslinjeId(utbetalingslinjeId),
        type = type,
        utbetalingId = utbetalingId,
        erstatterId = erstatter?.let { UtbetalingslinjeId(it) },
        opprettet = opprettet,
        sakId = sakId,
        periode =
            PeriodeForUtbetaling(
                fra = periodeFra,
                til = periodeTil,
            ),
        beloep = beloep,
        klassifikasjonskode = klassifikasjonskode,
        kjoereplan = kjoereplan,
    )

fun utbetalingMedOpphoer() =
    utbetaling(
        utbetalingslinjer =
            listOf(
                utbetalingslinje(
                    utbetalingId = UUID.randomUUID(),
                    sakId = SakId(1),
                    utbetalingslinjeId = 1,
                    type = Utbetalingslinjetype.OPPHOER,
                    beloep = null,
                ),
            ),
    )

fun utbetalingshendelse(
    id: UUID = UUID.randomUUID(),
    utbetalingId: UUID = UUID.randomUUID(),
    tidspunkt: Tidspunkt = Tidspunkt.now(),
    status: UtbetalingStatus = UtbetalingStatus.MOTTATT,
) = Utbetalingshendelse(id, utbetalingId, tidspunkt, status)

fun mockKonsistensavstemming(
    dag: LocalDate = LocalDate.now(),
    loependeUtbetalinger: List<OppdragForKonsistensavstemming>,
    id: UUIDBase64 = UUIDBase64(),
    opprettTilOgMed: Tidspunkt = dag.minusDays(1).atTime(LocalTime.MAX).toNorskTidspunkt(),
    sakType: Saktype = Saktype.BARNEPENSJON,
) = Konsistensavstemming(
    id = id,
    sakType = sakType,
    opprettet = Tidspunkt.now(),
    avstemmingsdata = null,
    loependeFraOgMed = Tidspunkt.ofNorskTidssone(dag, LocalTime.MIDNIGHT),
    opprettetTilOgMed = opprettTilOgMed,
    loependeUtbetalinger = loependeUtbetalinger,
)

fun oppdragForKonsistensavstemming(
    sakId: Long = 1,
    sakType: Saktype = Saktype.BARNEPENSJON,
    fnr: String = "123456",
    oppdragslinjeForKonsistensavstemming: List<OppdragslinjeForKonsistensavstemming>,
) = OppdragForKonsistensavstemming(
    sakId = SakId(sakId),
    sakType = sakType,
    fnr = Foedselsnummer(fnr),
    utbetalingslinjer = oppdragslinjeForKonsistensavstemming,
)

fun oppdragslinjeForKonsistensavstemming(
    id: Long = 1,
    opprettet: Tidspunkt = Tidspunkt.now(),
    fraOgMed: LocalDate,
    tilOgMed: LocalDate? = null,
    forrigeUtbetalingslinjeId: Long? = null,
    beloep: BigDecimal = BigDecimal(10000),
    attestanter: List<NavIdent> = listOf(NavIdent("attestant")),
    kjoereplan: Kjoereplan = Kjoereplan.MED_EN_GANG,
) = OppdragslinjeForKonsistensavstemming(
    id = UtbetalingslinjeId(id),
    opprettet = opprettet,
    fraOgMed = fraOgMed,
    tilOgMed = tilOgMed,
    forrigeUtbetalingslinjeId = forrigeUtbetalingslinjeId?.let { UtbetalingslinjeId(it) },
    beloep = beloep,
    attestanter = attestanter,
    kjoereplan = kjoereplan,
)
