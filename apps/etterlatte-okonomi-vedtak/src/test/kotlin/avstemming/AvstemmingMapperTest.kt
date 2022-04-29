package no.nav.etterlatte.avstemming

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.domain.Utbetalingsoppdrag
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.LocalDateTime
import java.time.Month
import java.util.*

internal class AvstemmingMapperTest {

    @Test
    fun `antall i grunnlagsdata skal telles opp korrekt for godkjent, varsel, avvist og mangler`() {
        val utbetalingsoppdragsliste = listOf(
            mockk<Utbetalingsoppdrag>(relaxed = true) {
                every { status } returns UtbetalingsoppdragStatus.GODKJENT
            },
            mockk(relaxed = true) {
                every { status } returns UtbetalingsoppdragStatus.GODKJENT
            },
            mockk(relaxed = true) {
                every { status } returns UtbetalingsoppdragStatus.GODKJENT_MED_FEIL
            },
            mockk(relaxed = true) {
                every { status } returns UtbetalingsoppdragStatus.AVVIST
            },
            mockk(relaxed = true) {
                every { status } returns UtbetalingsoppdragStatus.AVVIST
            },
            mockk(relaxed = true) {
                every { status } returns UtbetalingsoppdragStatus.AVVIST
            },
            mockk(relaxed = true) {
                every { status } returns UtbetalingsoppdragStatus.SENDT // telles som mangler
            },
            mockk(relaxed = true) {
                every { status } returns UtbetalingsoppdragStatus.FEILET // telles som mangler
            },
        )
        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdragsliste, UUID.randomUUID())
        val grunnlagsdata = avstemmingsdataMapper.grunnlagsdata(utbetalingsoppdragsliste)

        assertAll("Skal telle opp rett antall godkjent, varsel, avvist og mangler",
            { assertEquals(grunnlagsdata.godkjentAntall, 2) },
            { assertEquals(grunnlagsdata.varselAntall, 1) },
            { assertEquals(grunnlagsdata.avvistAntall, 3) },
            { assertEquals(grunnlagsdata.manglerAntall, 2) })
    }

    @Test
    fun `antall i grunnlagsdata skal vaere 0 for alle statuser`() {
        val utbetalingsoppdragsliste = emptyList<Utbetalingsoppdrag>()

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdragsliste, UUID.randomUUID())
        val grunnlagsdata = avstemmingsdataMapper.grunnlagsdata(utbetalingsoppdragsliste)

        assertAll("Skal telle opp rett antall godkjent, varsel, avvist og mangler",
            { assertEquals(grunnlagsdata.godkjentAntall, 0) },
            { assertEquals(grunnlagsdata.varselAntall, 0) },
            { assertEquals(grunnlagsdata.avvistAntall, 0) },
            { assertEquals(grunnlagsdata.manglerAntall, 0) })
    }

    @Test
    fun `foerste og siste avstemmingsnoekkel skal finnes fra utbetalingsoppdrag`() {
        val utbetalingsoppdragsliste = listOf(
            mockk<Utbetalingsoppdrag>(relaxed = true) {
                every { avstemmingsnoekkel } returns LocalDateTime.of(2020, Month.APRIL, 10, 14, 0, 0) // foerste
            },
            mockk(relaxed = true) {
                every { avstemmingsnoekkel } returns LocalDateTime.of(2020, Month.APRIL, 10, 14, 0, 0).plusDays(1)
            },
            mockk(relaxed = true) {
                every { avstemmingsnoekkel } returns LocalDateTime.of(2022, Month.JANUARY, 24, 22, 0, 0) // siste
            },
            mockk(relaxed = true) {
                every { avstemmingsnoekkel } returns LocalDateTime.of(2020, Month.APRIL, 10, 14, 0, 0).plusHours(1)
            },
            mockk(relaxed = true) {
                every { avstemmingsnoekkel } returns LocalDateTime.of(2020, Month.APRIL, 10, 14, 0, 0).plusMinutes(2)
            },
        )

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdragsliste, UUID.randomUUID())
        val periode = avstemmingsdataMapper.periode(utbetalingsoppdragsliste)
        assertAll("skal finne forste og siste avstemmingsnoekkel i liste",
            { assertEquals("2020041014", periode.start) },
            { assertEquals("2022012422", periode.endInclusive) }
        )
    }
}