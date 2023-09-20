package rapidsandrivers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import org.junit.jupiter.api.Test

internal class FeilhaandteringKtTest {
    @Test
    fun `feilhaandtering kaster ikke feilen videre, men publiserer på feilkø`() {
        val packet = JsonMessage("{}", MessageProblems(""))
        val context = mockk<MessageContext>().also { every { it.publish(any()) } returns Unit }
        withFeilhaandtering(packet, context, EventNames.OPPRETT_VEDTAK) {
            throw RuntimeException()
        }
        verify { context.publish(any()) }
    }

    @Test
    fun `feilhaandtering gjoer ingenting hvis ingenting feiler`() {
        val packet = JsonMessage("{}", MessageProblems(""))
        val context = mockk<MessageContext>().also { every { it.publish(any()) } returns Unit }
        withFeilhaandtering(packet, context, EventNames.OPPRETT_VEDTAK) {
        }
        verify(exactly = 0) { context.publish(any()) }
    }
}
