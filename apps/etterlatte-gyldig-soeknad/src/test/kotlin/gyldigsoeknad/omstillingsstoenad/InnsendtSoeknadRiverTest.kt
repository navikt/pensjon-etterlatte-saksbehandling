package gyldigsoeknad.omstillingsstoenad

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad.InnsendtSoeknadRiver
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InnsendtSoeknadRiverTest {

    private val behandlingClientMock = mockk<BehandlingClient>()
    private val inspector = TestRapid().apply {
        InnsendtSoeknadRiver(this, behandlingClientMock)
    }

    @Test
    fun `skal opprette sak, initiere foerstegangsbehandling og gyldighetsvurdering til klar for manuell vurdering`() {
        val sakId = 12345L
        val id = UUID.randomUUID()

        every {
            behandlingClientMock.skaffSak(any(), any())
        } returns Sak("21478247343", SakType.BARNEPENSJON, sakId, null)
        every { behandlingClientMock.initierBehandling(any(), any(), any()) } returns id
        every { behandlingClientMock.lagreGyldighetsVurdering(any(), any()) } returns Unit

        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        assertEquals(Opplysningstype.SOEKER_PDL_V1.name, inspector.message(0).get("@behov").asText())
        assertEquals(sakId.toString(), inspector.message(0).get("sakId").asText())
        assertEquals("26058411891", inspector.message(0).get("fnr").asText())
        assertEquals(PersonRolle.GJENLEVENDE.name, inspector.message(0).get("rolle").asText())

        assertEquals(Opplysningstype.AVDOED_PDL_V1.name, inspector.message(1).get("@behov").asText())
        assertEquals(sakId.toString(), inspector.message(1).get("sakId").asText())
        assertEquals("21478247343", inspector.message(1).get("fnr").asText())
        assertEquals(PersonRolle.AVDOED.name, inspector.message(1).get("rolle").asText())

        assertEquals("soeknad_innsendt", inspector.message(2).get(EVENT_NAME_KEY).asText())
        assertEquals(sakId, inspector.message(2).get(GyldigSoeknadVurdert.sakIdKey).longValue())
        assertEquals(id.toString(), inspector.message(2).get(GyldigSoeknadVurdert.behandlingIdKey).asText())

        assertEquals(3, inspector.size)
    }

    companion object {
        private val melding = readFile("/innsendtsoeknad.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }
}