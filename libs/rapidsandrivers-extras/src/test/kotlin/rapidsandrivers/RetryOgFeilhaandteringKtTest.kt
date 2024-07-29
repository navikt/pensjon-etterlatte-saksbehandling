package no.nav.etterlatte.rapidsandrivers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RetryOgFeilhaandteringKtTest {
    @Test
    fun `feilhaandtering kaster ikke feilen videre, men publiserer på feilkø`() {
        val packet =
            JsonMessage("{}", MessageProblems(""))
        val context = mockk<MessageContext>().also { every { it.publish(any()) } returns Unit }
        var antallForsoek = 0
        withRetryOgFeilhaandtering(
            packet = packet,
            context = context,
            feilendeSteg = ReguleringHendelseType.BEREGNA.lagEventnameForType(),
            kontekst = Kontekst.REGULERING,
        ) {
            antallForsoek++
            throw RuntimeException()
        }
        verify(exactly = 1) { context.publish(any()) }
        assertEquals(Kontekst.REGULERING.retries + 1, antallForsoek)
    }

    @Test
    fun `feilhaandtering gjoer ingenting hvis ingenting feiler`() {
        val packet = JsonMessage("{}", MessageProblems(""))
        val context = mockk<MessageContext>().also { every { it.publish(any()) } returns Unit }
        var antallForsoek = 0
        withRetryOgFeilhaandtering(
            packet = packet,
            context = context,
            feilendeSteg = ReguleringHendelseType.BEREGNA.lagEventnameForType(),
            kontekst = Kontekst.REGULERING,
        ) {
            antallForsoek++
        }
        verify(exactly = 0) { context.publish(any()) }
        assertEquals(1, antallForsoek)
    }

    @Test
    fun `Feilhaandtering skal kjoere ny retry hvis feiler for doedshendelse`() {
        val packet =
            JsonMessage("{}", MessageProblems(""))
        val context = mockk<MessageContext>().also { every { it.publish(any()) } returns Unit }
        var antallForsoek = 0
        withRetryOgFeilhaandtering(
            packet = packet,
            context = context,
            feilendeSteg = ReguleringHendelseType.BEREGNA.lagEventnameForType(),
            kontekst = Kontekst.DOEDSHENDELSE,
        ) {
            antallForsoek++
            throw RuntimeException()
        }
        verify(exactly = 1) { context.publish(any()) }
        assertEquals(Kontekst.DOEDSHENDELSE.retries + 1, antallForsoek)
    }
}
