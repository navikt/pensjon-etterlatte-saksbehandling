package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.nav.etterlatte.utbetaling.common.tidssoneNorge
import no.nav.etterlatte.utbetaling.common.toTidspunkt
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Fortegn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.Instant
import java.time.LocalDateTime
import java.time.Month
import java.time.temporal.ChronoUnit
import java.math.BigDecimal

internal class AvstemmingsdataMapperTest {

    @Test
    fun `skal opprette avstemming fra utbetalingsoppdrag med startmelding, datamelding og sluttmelding`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalingsoppdrag = listOf(utbetaling(status = UtbetalingStatus.FEILET))

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, fraOgMed, til, "1")
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val (startMelding, dataMelding, sluttMelding) = avstemmingsmelding

        assertEquals(AksjonType.START, startMelding.aksjon.aksjonType)
        assertEquals(AksjonType.DATA, dataMelding.aksjon.aksjonType)
        assertEquals(AksjonType.AVSL, sluttMelding.aksjon.aksjonType)
    }

    @Test
    fun `skal skal splitte opp datameldinger slik at de kun inneholder et gitt antall detaljer`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalingsoppdrag = listOf(
            utbetaling(id = 1, status = UtbetalingStatus.FEILET),
            utbetaling(id = 2, status = UtbetalingStatus.FEILET),
            utbetaling(id = 3, status = UtbetalingStatus.FEILET),
        )

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, fraOgMed, til, "1", 2)
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val (_, dataMelding1, dataMelding2, _) = avstemmingsmelding

        assertEquals(AksjonType.DATA, dataMelding1.aksjon.aksjonType)
        assertEquals(2, dataMelding1.detalj.size)

        assertEquals(AksjonType.DATA, dataMelding2.aksjon.aksjonType)
        assertEquals(1, dataMelding2.detalj.size)
    }

    @Test
    fun `skal kun inneholde grunnadata, total og periode i forste datamelding`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalingsoppdrag = listOf(
            utbetaling(id = 1, status = UtbetalingStatus.FEILET),
            utbetaling(id = 2, status = UtbetalingStatus.FEILET),
            utbetaling(id = 3, status = UtbetalingStatus.FEILET),
        )

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, fraOgMed, til, "1", 2)
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val (_, dataMelding1, dataMelding2, _) = avstemmingsmelding

        assertNotNull(dataMelding1.grunnlag)
        assertNotNull(dataMelding1.total)
        assertNotNull(dataMelding1.periode)

        assertEquals(3, dataMelding1.total.totalAntall)

        assertNull(dataMelding2.grunnlag)
        assertNull(dataMelding2.total)
        assertNull(dataMelding2.periode)
    }

    @Test
    fun `skal returnere riktig totalantall, totalbelop og fortegn`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalingsoppdrag = listOf(
            utbetaling(id = 1, status = UtbetalingStatus.SENDT),
            utbetaling(id = 2, status = UtbetalingStatus.GODKJENT),
            utbetaling(id = 3, status = UtbetalingStatus.GODKJENT_MED_FEIL),
            utbetaling(id = 4, status = UtbetalingStatus.AVVIST),
            utbetaling(id = 5, status = UtbetalingStatus.FEILET),
        )

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, fraOgMed, til, "1")
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val (_, dataMelding, _) = avstemmingsmelding

        assertEquals(5, dataMelding.total.totalAntall)
        assertEquals(BigDecimal(50_000), dataMelding.total.totalBelop)
        assertEquals(Fortegn.T, dataMelding.total.fortegn)
    }

    @Test
    fun `antall i grunnlagsdata skal telles opp korrekt for godkjent, varsel, avvist og mangler`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalingsoppdragsliste = listOf(
            utbetaling(id = 1, status = UtbetalingStatus.GODKJENT),
            utbetaling(id = 2, status = UtbetalingStatus.GODKJENT),
            utbetaling(id = 3, status = UtbetalingStatus.GODKJENT_MED_FEIL),
            utbetaling(id = 4, status = UtbetalingStatus.AVVIST),
            utbetaling(id = 5, status = UtbetalingStatus.AVVIST),
            utbetaling(id = 6, status = UtbetalingStatus.AVVIST),
            utbetaling(id = 7, status = UtbetalingStatus.SENDT),
            utbetaling(id = 8, status = UtbetalingStatus.FEILET),
        )
        val avstemmingsdataMapper = AvstemmingsdataMapper(
            utbetalinger = utbetalingsoppdragsliste, fraOgMed = fraOgMed, til = til, avstemmingId = "1"
        )
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()
        val (_, dataMelding, _) = avstemmingsmelding

        assertAll("Skal telle opp rett antall godkjent, varsel, avvist og mangler",
            { assertEquals(2, dataMelding.grunnlag.godkjentAntall) },
            { assertEquals(BigDecimal(20_000), dataMelding.grunnlag.godkjentBelop) },
            { assertEquals(Fortegn.T, dataMelding.grunnlag.godkjentFortegn) },

            { assertEquals(1, dataMelding.grunnlag.varselAntall) },
            { assertEquals(BigDecimal(10_000), dataMelding.grunnlag.varselBelop) },
            { assertEquals(Fortegn.T, dataMelding.grunnlag.varselFortegn) },

            { assertEquals(4, dataMelding.grunnlag.avvistAntall) },
            { assertEquals(BigDecimal(40_000), dataMelding.grunnlag.avvistBelop) },
            { assertEquals(Fortegn.T, dataMelding.grunnlag.avvistFortegn) },

            { assertEquals(1, dataMelding.grunnlag.manglerAntall) },
            { assertEquals(BigDecimal(10_000), dataMelding.grunnlag.manglerBelop) },
            { assertEquals(Fortegn.T, dataMelding.grunnlag.manglerFortegn) }
        )
    }

    @Test
    fun `antall i grunnlagsdata skal vaere 0 for alle statuser naar det er ingen utbetalinger aa avstemme`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalingsoppdgragsliste = emptyList<Utbetaling>()

        val avstemmingsdataMapper = AvstemmingsdataMapper(
            utbetalinger = utbetalingsoppdgragsliste, fraOgMed = fraOgMed, til = til, avstemmingId = "1"
        )
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()
        val (_, dataMelding, _) = avstemmingsmelding

        assertAll("Antall i grunnlagsdato skal vaere 0 for alle statuser",
            { assertEquals(0, dataMelding.grunnlag.godkjentAntall) },
            { assertEquals(0, dataMelding.grunnlag.varselAntall) },
            { assertEquals(0, dataMelding.grunnlag.avvistAntall) },
            { assertEquals(0, dataMelding.grunnlag.manglerAntall) })
    }

    @Test
    fun `antall meldinger skal vaere satt til 0 naar det er ingen utbetalinger aa avstemme`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalingsoppdgragsliste = emptyList<Utbetaling>()

        val avstemmingsdataMapper = AvstemmingsdataMapper(
            utbetalinger = utbetalingsoppdgragsliste, fraOgMed = fraOgMed, til = til, avstemmingId = "1"
        )
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()
        val (_, dataMelding, _) = avstemmingsmelding

        assertEquals(0, dataMelding.total.totalAntall)
    }

    @Test
    fun `nokkelTom og nokkelFom er satt til 0 naar det er ingen utbetalinger aa avstemme`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalingsoppdgragsliste = emptyList<Utbetaling>()

        val avstemmingsdataMapper = AvstemmingsdataMapper(
            utbetalinger = utbetalingsoppdgragsliste, fraOgMed = fraOgMed, til = til, avstemmingId = "1"
        )
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()
        val (startmelding, dataMelding, sluttmelding) = avstemmingsmelding

        assertAll(
            "nokkelTom og nokkelFom er satt til 0 naar det er ingen utbetalinger aa avstemme",
            { assertEquals("0", startmelding.aksjon.nokkelFom) },
            { assertEquals("0", startmelding.aksjon.nokkelTom) },
            { assertEquals("0", dataMelding.aksjon.nokkelFom) },
            { assertEquals("0", dataMelding.aksjon.nokkelFom) },
            { assertEquals("0", sluttmelding.aksjon.nokkelTom) },
            { assertEquals("0", sluttmelding.aksjon.nokkelTom) },
        )
    }


    @Test
    fun `foerste og siste avstemmingsnoekkel skal finnes fra utbetalingsoppdrag`() {
        val fraOgMed = (LocalDateTime.of(2020, Month.APRIL, 10, 14, 0, 0).minusDays(1).toTidspunkt(tidssoneNorge()))
        val til = LocalDateTime.of(2022, Month.JANUARY, 24, 22, 0, 0).plusHours(1).toTidspunkt(tidssoneNorge())

        val utbetalingsoppdragsliste = listOf(
            utbetaling(id = 1, avstemmingsnoekkel = LocalDateTime.of(2020, Month.APRIL, 10, 14, 0, 0).toTidspunkt(tidssoneNorge())),
            utbetaling(id = 2, avstemmingsnoekkel = LocalDateTime.of(2020, Month.APRIL, 10, 14, 0, 0).plusDays(1).toTidspunkt(tidssoneNorge())),
            utbetaling(id = 3, avstemmingsnoekkel = LocalDateTime.of(2022, Month.JANUARY, 24, 22, 0, 0).toTidspunkt(tidssoneNorge())),
            utbetaling(id = 4, avstemmingsnoekkel = LocalDateTime.of(2020, Month.APRIL, 10, 14, 0, 0).plusHours(1).toTidspunkt(tidssoneNorge())),
            utbetaling(id = 5, avstemmingsnoekkel = LocalDateTime.of(2020, Month.APRIL, 10, 14, 0, 0).plusMinutes(2).toTidspunkt(tidssoneNorge())),
        )

        val avstemmingsdataMapper = AvstemmingsdataMapper(
            utbetalinger = utbetalingsoppdragsliste, fraOgMed = fraOgMed, til = til, avstemmingId = "1"
        )
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()
        val (_, dataMelding, _) = avstemmingsmelding
        assertAll("skal finne forste og siste avstemmingsnoekkel i liste",
            { assertEquals("2020040914", dataMelding.periode.datoAvstemtFom) },
            { assertEquals("2022012422", dataMelding.periode.datoAvstemtTom) })
    }
}