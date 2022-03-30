package no.nav.etterlatte.common

import no.nav.etterlatte.dummyAttestasjon
import no.nav.etterlatte.dummyVedtak
import no.nav.etterlatte.oppdrag.OppdragMapper
import no.nav.etterlatte.readFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class JaxbTest {

    @Test
    fun `should generate xml from oppdrag`() {
        val oppdrag = OppdragMapper.oppdragFraVedtak(dummyVedtak(), dummyAttestasjon())
        val oppdragAsXml = Jaxb.toXml(oppdrag)

        assertNotNull(oppdragAsXml)
    }

    @Test
    fun `should convert xml to oppdrag`() {
        val oppdragXml = readFile("/oppdrag_ugyldig.xml")
        val oppdrag = Jaxb.toOppdrag(oppdragXml)

        assertNotNull(oppdrag)
        assertEquals("08", oppdrag.mmel.alvorlighetsgrad)
    }
}