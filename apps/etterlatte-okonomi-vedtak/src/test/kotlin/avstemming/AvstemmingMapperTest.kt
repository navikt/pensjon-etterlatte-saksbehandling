package no.nav.etterlatte.avstemming

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.domain.Utbetalingsoppdrag
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.*

internal class AvstemmingMapperTest {

    @Test
    fun `antall i grunnlagsdata opptelt korrekt for godkent, varsel, avvist og mangler`() {

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
    fun `antall i grunnlagsdata er 0 for alle statuser`() {

        val utbetalingsoppdragsliste = emptyList<Utbetalingsoppdrag>()

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdragsliste, UUID.randomUUID())
        val grunnlagsdata = avstemmingsdataMapper.grunnlagsdata(utbetalingsoppdragsliste)

        assertAll("Skal telle opp rett antall godkjent, varsel, avvist og mangler",
            { assertEquals(grunnlagsdata.godkjentAntall, 0) },
            { assertEquals(grunnlagsdata.varselAntall, 0) },
            { assertEquals(grunnlagsdata.avvistAntall, 0) },
            { assertEquals(grunnlagsdata.manglerAntall, 0) })
    }
}