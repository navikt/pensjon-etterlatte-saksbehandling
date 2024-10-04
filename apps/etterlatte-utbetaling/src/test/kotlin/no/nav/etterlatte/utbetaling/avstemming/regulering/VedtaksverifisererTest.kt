package no.nav.etterlatte.utbetaling.avstemming.regulering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.utbetaling.DatabaseExtension
import no.nav.etterlatte.utbetaling.avstemming.vedtak.Vedtaksverifiserer
import no.nav.etterlatte.utbetaling.common.UUID30
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Attestasjon
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.BehandlingId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kjoereplan
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.NavIdent
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdragKlassifikasjonskode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Periode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.PeriodeForUtbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Sak
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinjetype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsperiode
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingsperiodeType
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsvedtak
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.VedtakFattet
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.VedtakId
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class VedtaksverifisererTest(
    private val dataSource: DataSource,
) {
    private val saksbehandler = "Saksbehandler1"
    private val attestant = "Attestant1"
    private val enhet = Enhetsnummer("1234")

    private var teller = 0L
    private var vedtakTeller = 1L

    @Test
    fun `sjekker utbetalingsperioder opp mot vedtak`() {
        val dao = UtbetalingDao(dataSource)

        val sak =
            Sak(
                ident = "Sak1",
                id = 1L,
                sakType = Saktype.BARNEPENSJON,
            )

        val foerRegulering =
            opprettUtbetaling(
                sak,
                listOf(
                    UtbetalingslinjeRequest(
                        null,
                        beloep = BigDecimal(1000),
                        fra = YearMonth.of(2024, Month.JANUARY),
                        til = YearMonth.of(2024, Month.APRIL),
                    ),
                    UtbetalingslinjeRequest(
                        null,
                        beloep = BigDecimal(1200),
                        fra = YearMonth.of(2024, Month.MAY),
                        til = YearMonth.of(2024, Month.JUNE),
                    ),
                ),
            ).also { dao.opprettUtbetaling(it.second) }
        val etterRegulering =
            opprettUtbetaling(
                sak,
                listOf(
                    UtbetalingslinjeRequest(
                        foerRegulering.second.utbetalingslinjer[0]
                            .id.value,
                        beloep = BigDecimal(1500),
                        fra = YearMonth.of(2024, Month.JANUARY),
                        til = YearMonth.of(2024, Month.APRIL),
                    ),
                    UtbetalingslinjeRequest(
                        null,
                        beloep = BigDecimal(1800),
                        fra = YearMonth.of(2024, Month.MAY),
                        til = YearMonth.of(2024, Month.JUNE),
                    ),
                ),
            ).also { dao.opprettUtbetaling(it.second) }

        val verifiserer = Vedtaksverifiserer()
        runBlocking {
            verifiserer.verifiser(foerRegulering.second, foerRegulering.first)
            verifiserer.verifiser(etterRegulering.second, etterRegulering.first)
        }
    }

    private fun opprettUtbetaling(
        sak: Sak,
        linjer: List<UtbetalingslinjeRequest>,
    ): Pair<Utbetalingsvedtak, Utbetaling> {
        val behandlingId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val behandling =
            Behandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                id = behandlingId,
            )
        val utbetaling =
            Utbetaling(
                id = utbetalingId,
                sakId = SakId(sak.id),
                sakType = Saktype.BARNEPENSJON,
                behandlingId = behandlingId.let { BehandlingId(it, UUID30(it.toString())) },
                vedtakId = VedtakId(vedtakTeller),
                opprettet = Tidspunkt.now(),
                endret = Tidspunkt.now(),
                avstemmingsnoekkel = Tidspunkt.now(),
                stoenadsmottaker = Foedselsnummer(SOEKER_FOEDSELSNUMMER.value),
                saksbehandlerEnhet = enhet,
                attestant = NavIdent(attestant),
                attestantEnhet = enhet,
                vedtak =
                    Utbetalingsvedtak(
                        vedtakId = vedtakTeller,
                        sak = sak,
                        behandling =
                        behandling,
                        pensjonTilUtbetaling = listOf(),
                        vedtakFattet =
                            VedtakFattet(
                                ansvarligSaksbehandler = saksbehandler,
                                ansvarligEnhet = enhet,
                            ),
                        attestasjon =
                            Attestasjon(
                                attestant = attestant,
                                attesterendeEnhet = enhet,
                            ),
                    ),
                saksbehandler = NavIdent(saksbehandler),
                oppdrag = Oppdrag(),
                utbetalingshendelser = listOf(),
                utbetalingslinjer =
                    linjer.map { linje ->
                        Utbetalingslinje(
                            id = UtbetalingslinjeId(teller++),
                            type = Utbetalingslinjetype.UTBETALING,
                            utbetalingId = utbetalingId,
                            erstatterId = linje.erstatterId?.let { UtbetalingslinjeId(it) },
                            opprettet = Tidspunkt.now(),
                            sakId = SakId(1L),
                            periode = PeriodeForUtbetaling(fra = linje.fra.atDay(1), til = linje.til?.atEndOfMonth()),
                            klassifikasjonskode = OppdragKlassifikasjonskode.BARNEPENSJON_OPTP,
                            kjoereplan = Kjoereplan.MED_EN_GANG,
                            beloep = linje.beloep,
                        )
                    },
            )
        val vedtak =
            Utbetalingsvedtak(
                vedtakId = vedtakTeller,
                sak = sak,
                behandling = behandling,
                pensjonTilUtbetaling =
                    linjer.map {
                        Utbetalingsperiode(
                            id = linjer.indexOf(it).toLong(),
                            periode = Periode(fom = it.fra, tom = it.til),
                            beloep = it.beloep,
                            type = UtbetalingsperiodeType.UTBETALING,
                            regelverk = Regelverk.fraDato(it.fra.atDay(1)),
                        )
                    },
                vedtakFattet =
                    VedtakFattet(
                        ansvarligSaksbehandler = saksbehandler,
                        ansvarligEnhet = enhet,
                    ),
                attestasjon =
                    Attestasjon(
                        attestant = attestant,
                        attesterendeEnhet = enhet,
                    ),
            )
        vedtakTeller++
        return Pair(vedtak, utbetaling)
    }
}

data class UtbetalingslinjeRequest(
    val erstatterId: Long?,
    val beloep: BigDecimal?,
    val fra: YearMonth,
    val til: YearMonth?,
)
