package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.attestasjon
import no.nav.etterlatte.readFile
import no.nav.etterlatte.vedtak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingJaxbTest {

    @Test
    fun `should generate xml from oppdrag`() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak(), attestasjon(), LocalDateTime.now())
        val oppdragAsXml = UtbetalingJaxb.toXml(oppdrag)

        assertNotNull(oppdragAsXml)
    }

    @Test
    fun `should convert xml to oppdrag`() {
        val oppdragXml = readFile("/oppdrag_ugyldig.xml")
        val oppdrag = UtbetalingJaxb.toOppdrag(oppdragXml)

        assertNotNull(oppdrag)
        assertEquals("08", oppdrag.mmel.alvorlighetsgrad)
    }
}