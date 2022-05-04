package no.nav.etterlatte.iverksetting.utbetaling

import no.nav.etterlatte.attestasjon
import no.nav.etterlatte.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.readFile
import no.nav.etterlatte.vedtak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class OppdragJaxbTest {

    @Test
    fun `should generate xml from oppdrag`() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak(), attestasjon(), LocalDateTime.now())
        val oppdragAsXml = OppdragJaxb.toXml(oppdrag)

        assertNotNull(oppdragAsXml)
    }

    @Test
    fun `should convert xml to oppdrag`() {
        val oppdragXml = readFile("/oppdrag_ugyldig.xml")
        val oppdrag = OppdragJaxb.toOppdrag(oppdragXml)

        assertNotNull(oppdrag)
        assertEquals("08", oppdrag.mmel.alvorlighetsgrad)
    }
}