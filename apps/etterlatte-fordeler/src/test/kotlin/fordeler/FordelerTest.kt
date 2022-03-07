package no.nav.etterlatte.fordeler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.readFile
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FordelerTest {

    private val fordelerService = mockk<FordelerService>()

    @Test
    fun `skal fordele gyldig soknad til behandling`() {
        every { fordelerService.sjekkGyldighetForBehandling(any()) } returns FordelerResultat.GyldigForBehandling

        val inspector = TestRapid()
            .apply { fordeler(this) }
            .apply { sendTestMessage(BARNEPENSJON_SOKNAD) }
            .inspektør

        assertEquals("ey_fordelt", inspector.message(0).get("@event_name").asText())
        assertEquals("true", inspector.message(0).get("@soeknad_fordelt").asText())
    }

    @Test
    fun `skal ikke fordele ugyldig soknad til behandling`() {
        every { fordelerService.sjekkGyldighetForBehandling(any()) } returns
                FordelerResultat.IkkeGyldigForBehandling("Ikke gyldig for behandling")

        val inspector = TestRapid()
            .apply { fordeler(this) }
            .apply { sendTestMessage(BARNEPENSJON_SOKNAD) }
            .inspektør

        assertEquals(0, inspector.size)
    }

    @Test
    fun `skal ikke sjekke soknad av annen type enn barnepensjon`() {
        val inspector = TestRapid()
            .apply { fordeler(this) }
            .apply { sendTestMessage(GJENLEVENDE_SOKNAD) }
            .inspektør

        assertEquals(0, inspector.size)
        verify(exactly = 0) { fordelerService.sjekkGyldighetForBehandling(any()) }
    }

    @Test
    fun `skal ikke sjekke soknad av annen versjon enn versjon 2`() {
        val inspector = TestRapid()
            .apply { fordeler(this) }
            .apply { sendTestMessage(UGYLDIG_VERSJON) }
            .inspektør

        assertEquals(0, inspector.size)
        verify(exactly = 0) { fordelerService.sjekkGyldighetForBehandling(any()) }
    }

    private fun fordeler(rapidsConnection: RapidsConnection) = Fordeler(rapidsConnection, fordelerService)

    companion object {
        val BARNEPENSJON_SOKNAD = readFile("/fordeler/soknad_barnepensjon.json")
        val GJENLEVENDE_SOKNAD = readFile("/fordeler/soknad_ikke_barnepensjon.json")
        val UGYLDIG_VERSJON = readFile("/fordeler/soknad_ugyldig_versjon.json")
    }

}
