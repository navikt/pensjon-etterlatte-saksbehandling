package no.nav.etterlatte.fordeler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_HAR_YRKESSKADE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_FOR_GAMMELT
import no.nav.etterlatte.readFile
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FordelerTest {

    private val fordelerService = mockk<FordelerService>()
    private val fordelerMetricLogger = mockk<FordelerMetricLogger>()
    private val inspector = TestRapid().apply { Fordeler(this, fordelerService, fordelerMetricLogger) }

    @Test
    fun `skal fordele gyldig soknad til behandling`() {
        every { fordelerService.sjekkGyldighetForBehandling(any()) } returns FordelerResultat.GyldigForBehandling

        val inspector = inspector.apply { sendTestMessage(BARNEPENSJON_SOKNAD) }.inspektør

        assertEquals("FORDELER:FORDELT", inspector.message(0).get("@event_name").asText())
        assertEquals("true", inspector.message(0).get("soeknadFordelt").asText())

        verify { fordelerMetricLogger.logMetricFordelt() }
    }

    @Test
    fun `skal ikke fordele ugyldig soknad til behandling`() {
        every { fordelerService.sjekkGyldighetForBehandling(any()) } returns
                FordelerResultat.UgyldigHendelse("Ikke gyldig for behandling")

        val inspector = inspector.apply { sendTestMessage(BARNEPENSJON_SOKNAD) }.inspektør

        assertEquals(0, inspector.size)
    }

    @Test
    fun `skal ikke fordele soknad som ikke oppfyller alle kriterier til behandling`() {
        every { fordelerService.sjekkGyldighetForBehandling(any()) } returns
                FordelerResultat.IkkeGyldigForBehandling(listOf(BARN_ER_FOR_GAMMELT, AVDOED_HAR_YRKESSKADE))

        val inspector = inspector.apply { sendTestMessage(BARNEPENSJON_SOKNAD) }.inspektør

        assertEquals(0, inspector.size)

        verify(exactly = 1) { fordelerMetricLogger.logMetricIkkeFordelt(match {
            it.ikkeOppfylteKriterier.containsAll(listOf(BARN_ER_FOR_GAMMELT, AVDOED_HAR_YRKESSKADE))
        }) }
    }

    @Test
    fun `skal ikke sjekke soknad av annen type enn barnepensjon`() {
        val inspector = inspector.apply { sendTestMessage(GJENLEVENDE_SOKNAD) }.inspektør

        assertEquals(0, inspector.size)
        verify(exactly = 0) { fordelerService.sjekkGyldighetForBehandling(any()) }
    }

    @Test
    fun `skal ikke sjekke soknad av annen versjon enn versjon 2`() {
        val inspector = inspector.apply { sendTestMessage(UGYLDIG_VERSJON) }.inspektør

        assertEquals(0, inspector.size)
        verify(exactly = 0) { fordelerService.sjekkGyldighetForBehandling(any()) }
    }

    companion object {
        val BARNEPENSJON_SOKNAD = readFile("/fordeler/soknad_barnepensjon.json")
        val GJENLEVENDE_SOKNAD = readFile("/fordeler/soknad_ikke_barnepensjon.json")
        val UGYLDIG_VERSJON = readFile("/fordeler/soknad_ugyldig_versjon.json")
    }
}
