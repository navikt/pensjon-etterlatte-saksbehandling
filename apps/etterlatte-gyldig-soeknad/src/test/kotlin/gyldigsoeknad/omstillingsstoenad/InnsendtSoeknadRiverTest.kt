package gyldigsoeknad.omstillingsstoenad

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad.InnsendtSoeknadRiver
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InnsendtSoeknadRiverTest {

    private val behandlingClientMock = mockk<BehandlingClient>()
    private val inspector = TestRapid().apply {
        InnsendtSoeknadRiver(this, behandlingClientMock)
    }

    @Test
    fun `skal opprette sak, initiere foerstegangsbehandling og gyldighetsvurdering til klar for manuell vurdering`() {
        val id = UUID.randomUUID()

        every { behandlingClientMock.skaffSak(any(), any()) } returns 4
        every { behandlingClientMock.initierBehandling(any(), any(), any()) } returns id
        every { behandlingClientMock.lagreGyldighetsVurdering(any(), any()) } returns Unit

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        assertEquals("soeknad_innsendt", inspector.first().get(EVENT_NAME_KEY).asText())
        assertEquals(4, inspector.first().get(GyldigSoeknadVurdert.sakIdKey).longValue())
        assertEquals(id.toString(), inspector.first().get(GyldigSoeknadVurdert.behandlingIdKey).asText())
        assertEquals(false, inspector.first().get(GyldigSoeknadVurdert.gyldigInnsenderKey).asBoolean())
    }

    private fun TestRapid.RapidInspector.first() = this.message(0)

    companion object {
        private val melding = readFile("/innsendtsoeknad.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }
}