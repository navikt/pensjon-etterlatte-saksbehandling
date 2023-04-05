package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import rapidsandrivers.migrering.MigreringRequest
import rapidsandrivers.migrering.Migreringshendelser
import rapidsandrivers.migrering.PesysId

class MigreringTest {

    @Test
    fun `skal sende migreringsmelding for hver enkelt sak`() {
        val melding = JsonMessage.newMessage(
            mapOf(EVENT_NAME_KEY to Migreringshendelser.START_MIGRERING)
        )
        val inspector = TestRapid().apply { Migrering(this, PesysRepository()) }
        inspector.sendTestMessage(melding.toJson())

        val melding1 = inspector.inspektør.message(0)

        val request = objectMapper.readValue(melding1.get("request").asText(), MigreringRequest::class.java)
        Assertions.assertEquals(PesysId("1234"), request.pesysId)
    }
}