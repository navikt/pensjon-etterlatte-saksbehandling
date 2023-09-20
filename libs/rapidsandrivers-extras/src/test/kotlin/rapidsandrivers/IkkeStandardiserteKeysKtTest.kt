package rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

class IkkeStandardiserteKeysKtTest {
    @Test
    fun `behandlingsid-liste blir lik foer og etter`() {
        val melding = JsonMessage.newMessage(mapOf())
        val ider = listOf<UUID>(UUID.randomUUID(), UUID.randomUUID())
        melding.tilbakestilteBehandlinger = ider
        Assertions.assertEquals(ider, melding.tilbakestilteBehandlinger)
    }

    @Test
    fun `kan ha tom behandlingsid-liste`() {
        val melding = JsonMessage.newMessage(mapOf())
        val ider = listOf<UUID>()
        melding.tilbakestilteBehandlinger = ider
        Assertions.assertEquals(ider, melding.tilbakestilteBehandlinger)
    }
}
