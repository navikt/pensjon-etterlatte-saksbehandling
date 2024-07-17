package no.nav.etterlatte.rapidsandrivers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import org.junit.jupiter.api.Test

internal class RetryOgFeilhaandteringKtTest {
    @Test
    fun `feilhaandtering kaster ikke feilen videre, men publiserer på feilkø`() {
        val packet =
            JsonMessage("{}", MessageProblems("")).also {
                it.interestedIn(ANTALL_RETRIES_KEY)
            }
        val context = mockk<MessageContext>().also { every { it.publish(any()) } returns Unit }
        withRetryOgFeilhaandtering(
            packet = packet,
            context = context,
            feilendeSteg = ReguleringHendelseType.BEREGNA.lagEventnameForType(),
            kontekst = Kontekst.REGULERING,
        ) {
            throw RuntimeException()
        }
        verify(exactly = 1) { context.publish(any()) }
    }

    @Test
    fun `feilhaandtering gjoer ingenting hvis ingenting feiler`() {
        val packet = JsonMessage("{}", MessageProblems(""))
        val context = mockk<MessageContext>().also { every { it.publish(any()) } returns Unit }
        withRetryOgFeilhaandtering(
            packet = packet,
            context = context,
            feilendeSteg = ReguleringHendelseType.BEREGNA.lagEventnameForType(),
            kontekst = Kontekst.REGULERING,
        ) {
        }
        verify(exactly = 0) { context.publish(any()) }
    }
}
