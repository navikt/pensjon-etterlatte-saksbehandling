package vilkaarsvurdering

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.vilkaarsvurdering.Migrering
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.migrering.Migreringshendelser
import java.util.*

internal class MigreringTest {
    private val vilkaarsvurderingServiceMock = mockk<VilkaarsvurderingService> {
        coEvery { migrer(any()) } returns mockk()
    }
    private val testRapid = TestRapid()
        .apply { Migrering(this, vilkaarsvurderingServiceMock) }

    @Test
    fun `tar opp migrer vilkaarsvurdering-event, kopierer vilkaarsvurdering og poster ny BEREGN-melding`() {
        val behandlingId = UUID.randomUUID()

        val melding = JsonMessage.newMessage(
            mapOf(
                "@event_name" to Migreringshendelser.VILKAARSVURDER,
                "sakId" to 1,
                BEHANDLING_ID_KEY to behandlingId
            )
        ).toJson()
        testRapid.sendTestMessage(melding)

        coVerify(exactly = 1) {
            vilkaarsvurderingServiceMock.migrer(
                behandlingId
            )
        }
        with(testRapid.inspektør.message(0)) {
            assertEquals(Migreringshendelser.TRYGDETID, this["@event_name"].asText())
        }
    }
}