package no.nav.etterlatte.avstemming

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.domain.Utbetalingsoppdrag
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.utbetalingsoppdrag
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Fortegn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.*

internal class AvstemmingMapperTest {

    @Test
    fun `skal returnere tom liste dersom det ikke er noen utbetalingsoppdrag og avstemme`() {
        val utbetalingsoppdrag = emptyList<Utbetalingsoppdrag>()

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, UUID.randomUUID())
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        assertEquals(emptyList<Avstemmingsdata>(), avstemmingsmelding)
    }

    @Test
    fun `skal opprette avstemming fra utbetalingsoppdrag med startmelding, datamelding og sluttmelding`() {
        val utbetalingsoppdrag = listOf(utbetalingsoppdrag(status = UtbetalingsoppdragStatus.FEILET))

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, UUID.randomUUID())
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val (startMelding, dataMelding, sluttMelding) = avstemmingsmelding

        assertEquals(AksjonType.START, startMelding.aksjon.aksjonType)
        assertEquals(AksjonType.DATA, dataMelding.aksjon.aksjonType)
        assertEquals(AksjonType.AVSL, sluttMelding.aksjon.aksjonType)
    }

    @Test
    fun `skal skal splitte opp datameldinger slik at de kun inneholder et gitt antall detaljer`() {
        val utbetalingsoppdrag = listOf(
            utbetalingsoppdrag(id = 1, status = UtbetalingsoppdragStatus.FEILET),
            utbetalingsoppdrag(id = 2, status = UtbetalingsoppdragStatus.FEILET),
            utbetalingsoppdrag(id = 3, status = UtbetalingsoppdragStatus.FEILET),
        )

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, UUID.randomUUID(), 2)
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val (_, dataMelding1, dataMelding2, _) = avstemmingsmelding

        assertEquals(AksjonType.DATA, dataMelding1.aksjon.aksjonType)
        assertEquals(2, dataMelding1.detalj.size)

        assertEquals(AksjonType.DATA, dataMelding2.aksjon.aksjonType)
        assertEquals(1, dataMelding2.detalj.size)

    }

    @Test
    fun `skal kun inneholde grunnadata, total og periode i forste datamelding`() {
        val utbetalingsoppdrag = listOf(
            utbetalingsoppdrag(id = 1, status = UtbetalingsoppdragStatus.FEILET),
            utbetalingsoppdrag(id = 2, status = UtbetalingsoppdragStatus.FEILET),
            utbetalingsoppdrag(id = 3, status = UtbetalingsoppdragStatus.FEILET),
        )

        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, UUID.randomUUID(), 2)
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val (_, dataMelding1, dataMelding2, _) = avstemmingsmelding

        assertNotNull(dataMelding1.grunnlag)
        assertNotNull(dataMelding1.total)
        assertNotNull(dataMelding1.periode)

        assertEquals(3, dataMelding1.total.totalAntall)
        assertEquals(Fortegn.T, dataMelding1.total.fortegn)

        assertNull(dataMelding2.grunnlag)
        assertNull(dataMelding2.total)
        assertNull(dataMelding2.periode)
    }

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